package kr.co.sbh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 등록 엑티비티
 * 피보호자정보를 공유 설정환경에 저장한후
 * 폼 검증후 저장내용을 서버에 보낸다.
 * 서버에 전송 내역은 다음과 같다.
 *	<ul>
 *		<li>이름</li>
 *		<li>메세지</li>
 *		<li>보호자 전화번호</li>
 *		<li>이미지</li>
 */
public class ChildRegActivity extends BaseActivity implements OnClickListener {
	// element
	private EditText nameEt;			// 이름
	private EditText phoneEt;			// 보호자-1 전화
	private EditText subPhoneEt;		// 보호자-2 전화
	private EditText parentEmailEt;		// 보호자 이메일
	private EditText wardEmailEt;		// 피보호자 이메일
	private ImageView picIv;
	private SharedPreferences settings;

	private String mSdcardPath;
	private String selectedFile = null;
	private String imageUri;
	private final int TAKE_PICTURE = 4637;	// 사진 촬영 결과 코드
	//	ftp
	private MyFTPClient mFtp = null;
	// 쓰레드 처리 핸들러
    Handler handler = new Handler(){
    	@Override
		public void handleMessage(final Message msg){
    		if(msg.what == OK) {	// 처리가 완료 됐을떄 다음 화면으로 넘긴다.
    			// 공유환경 설정 열기
    	        SharedPreferences.Editor prefEditor = settings.edit();
    	        prefEditor.putBoolean("isChild", true);		// 아동모드로 등록
    	        prefEditor.commit();

    	        // 완료되면 메인페이지로
    	        Intent intent =  new Intent(ChildRegActivity.this, WardModeActivity.class);
    	        finish();
    			startActivity(intent);
    		}else{

    		}
    	}
    };

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.join_layout);
		settings = getSharedPreferences(PREFER, MODE_PRIVATE);
		// sdcard path 가져오기
		String extState = Environment.getExternalStorageState();
		if (extState.equals(Environment.MEDIA_MOUNTED)) {
			mSdcardPath = Environment.getExternalStorageDirectory().getPath();
		} else {
			mSdcardPath = Environment.MEDIA_UNMOUNTED;
		}

		initLayout();
	}


	/*
	 * 사진 촬영 혹은 사진 선택 후 엑티비티 결과
	 */
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			return;
		}

		// 사진 촬영선택에 uri가 없을 경우 bitmap을 이용해 파일을 만들어 준다.
		if (requestCode == TAKE_PICTURE && data.getData() == null) {
			String tmpFile = getDateTime() + ".jpg";
			Bitmap bm = (Bitmap) data.getExtras().get("data");
			try {
				// bitmap을 jpg로 압축해주자
				bm.compress(CompressFormat.JPEG, 100,
						openFileOutput(tmpFile, MODE_PRIVATE));
			} catch (FileNotFoundException e) {
				// 알림 다이얼로그
				new AlertDialog.Builder(this)
						.setMessage("선택한 파일을 가져올수가 없습니다.").setTitle("알림")
						.setPositiveButton("확인", null).show();
				selectedFile = null;
			}
			picIv.setImageURI(data.getData());
			selectedFile = this.getFilesDir() /* 앱 어플리케이션 저장 위치 */
					+ "/" + tmpFile;
		} else {
			selectedFile = getRealImagePath(data.getData());
			picIv.setImageURI(data.getData());
		}

		imageUri = data.getData().getPath();

		// 파일 길이 체크
		String file = selectedFile.substring(selectedFile.lastIndexOf("/") + 1);
		if (file.length() >= MAX_FILE_NAME_LENGTH) {
			new AlertDialog.Builder(this)
					.setMessage("이미지명이 너무 깁니다.\n 이미지 이름은 100자 이내로 해주세요.")
					.setTitle("알림").setPositiveButton("확인", null).show();
			selectedFile = null;
			return;
		}

		File tmpfile = null;
		tmpfile = new File(selectedFile); // File 객체 생성
		int fileSize = (int) tmpfile.length(); // File 객체의 length() 메서드로 파일 길이
												// 구하기
		if (fileSize > MAX_FILE_SIZE) {
			int size = MAX_FILE_SIZE / 1024 / 1024;
			new AlertDialog.Builder(this)
					.setMessage(
							"이미지 크기는 " + String.valueOf(size) + "MB 이내로 해주세요")
					.setTitle("알림").setPositiveButton("확인", null).show();
			selectedFile = null;
			return;
		}
	}

	/**
	 * 사진촬영 혹은 사진선택번호를 받아 해당 intent로 넘긴다.
	 * @param which
	 *
	 */
	public void attachFileFilter(final int which) {
		Intent intent = null;
		intent = new Intent();
		switch (which) {
		case 0: // 사진 촬영이면

			// intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

			startActivityForResult(intent, TAKE_PICTURE);
			break;
		case 1: // 사진 선택이면
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.setDataAndType(Uri.parse("*.jpg"), "image/*");
			startActivityForResult(intent, ACTION_RESULT);
			break;
		}

	}

	/**
	 * 엘리먼트 설정
	 */
	private void initLayout(){
		// 엘리먼트 후킹
		nameEt = (EditText)findViewById(R.id.join_name);
		phoneEt = (EditText)findViewById(R.id.phone1);
		subPhoneEt = (EditText)findViewById(R.id.phone2);
		picIv = (ImageView)findViewById(R.id.avata_image);
		parentEmailEt = (EditText)findViewById(R.id.parent_email);
		wardEmailEt = (EditText)findViewById(R.id.ward_email);
		ImageButton joinBtn = (ImageButton)findViewById(R.id.pic_reg_btn);
		Button regBtn = (Button)findViewById(R.id.register_btn);
		// 이벤트 설정
		joinBtn.setOnClickListener(this);
		regBtn.setOnClickListener(this);
	}


	@Override
	public void onClick(final View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case  R.id.pic_reg_btn :
			new AlertDialog.Builder(this).setTitle("첨부파일 선택")
				.setItems(R.array.attach,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							// 첨부파일 선택 처리 메소드
							attachFileFilter(which);
						}
					}).setNegativeButton("취소", null).show();
			break;
		case  R.id.register_btn :
			if(checkForm() != false){
				registerUser();
			}
			break;
		}
	}

	/*
	 * 파일 이름에 붙여줄 날자
	 */
	private String getDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Date date = new Date();
		return dateFormat.format(date);
	}

	/**
	 * URI로 부터 실제 파일 경로를 가져온다.
	 *
	 * @param uriPath
	 *            URI : URI 경로
	 * @return String : 실제 파일 경로
	 */
	private String getRealImagePath(final Uri uriPath) {
		String[] proj = { MediaStore.Images.Media.DATA };

		Cursor cursor = managedQuery(uriPath, proj, null, null, null);
		int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		cursor.moveToFirst();

		String path = cursor.getString(index);
		// path = path.substring(5);
		// 일부 motorola등에서 시작 path에 '/mnt/' 가 없을수 있음..
		return path.replace("/mnt/", "");
	}

	/*
	 * 기존 파일의 확장자를 새로운 파일에 붙여줌
	 */
	private String getExtension(final String oldFile, final String sep) {
		// 확장자 가져오기

		try{
			int index = oldFile.lastIndexOf(sep);
			String ext = oldFile.substring(index).toLowerCase();
			return ext;
		}catch(Exception e){
			return "";	//
		}
	}

	/**
	 * 폼 체크
	 */
	private boolean checkForm() {
		if(TextUtils.isEmpty(nameEt.getText())){
			Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}

		if(TextUtils.isEmpty(phoneEt.getText())){
			Toast.makeText(this, "전화번호를 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}

		if(TextUtils.isEmpty(subPhoneEt.getText())){
			Toast.makeText(this, "전화번호를 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}

		if(TextUtils.isEmpty(parentEmailEt.getText())){
			Toast.makeText(this, "보호자의 이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}

		if(MyUtils.isEmailValid(parentEmailEt.getText().toString()) == false){
			Toast.makeText(this, "보호자의 이메일을 정확히 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}


		if(TextUtils.isEmpty(wardEmailEt.getText())){
			Toast.makeText(this, "피보호자의 이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}

		if(MyUtils.isEmailValid(wardEmailEt.getText().toString()) == false){
			Toast.makeText(this, "피보호자의 이메일을 정확히 입력하세요.", Toast.LENGTH_SHORT).show();
			return false;
		}

		if(TextUtils.isEmpty(selectedFile)){
			Toast.makeText(this, "사용자 이미지를 등록하세요", Toast.LENGTH_SHORT).show();
			return false;
		}

		return true;
	}


	/**
	 * 서버에 피보호자 정보를 등록한다.
	 */
	private void registerUser() {
		// 입력된 정보를 받는 param
		JoinParams param = new JoinParams();
		param.name = nameEt.getText().toString();
		param.phone = phoneEt.getText().toString();
		param.subPhone = subPhoneEt.getText().toString();
		param.parentEmail = parentEmailEt.getText().toString();
		param.wardEmail = wardEmailEt.getText().toString();
		param.fileName = selectedFile;
		// 파라미터로 사용자 정보를 넘겨주고 쓰레드로 업로드처리한다.
		new AsyncTaskUserInfoUpload().execute(param);
	}

	/*
	 * ftp 연결 설정
	 */
	private boolean connectFTP() {
		mFtp = new MyFTPClient(SERVER_URL, SERVER_FTP_PORT, FTP_NAME,
				FTP_PASSWORD);
		if (!mFtp.connect()) {
			return false;
		}

		if (!mFtp.login()) {
			mFtp.logout();
			return false;
		}

		mFtp.cd(FTP_PATH);
		return true;
	}

	/**
	 *	사용자 이미지 파일 및 사용자 정보 업로드 클래스
	 *	서버에 사용자 정보와 이미지를 쓰레드 형식으로
	 *  업로드 한다.
	 */
	private class AsyncTaskUserInfoUpload extends
			AsyncTask<JoinParams, String, Boolean> {
		ProgressDialog dialog = null;
		private String receiveFiles;

		@Override
		protected void onPostExecute(final Boolean result) {	// 전송 완료후
			// 모든 파일이 전송이 완료되면 다이얼로그를 닫는다.
			dialog.dismiss(); // 프로그레스 다이얼로그 닫기
			if( ChildRegActivity.this.mFtp.isConnected()){	// 연결이 되어 있으면
				ChildRegActivity.this.mFtp.logout(); // 로그 아웃
			}
			// 파일 전송 결과를 출력
			if (result) { // 파일 전송이 정상이면

				// 공유환경에 유저 정보 저장
		        SharedPreferences settings = getSharedPreferences(PREFER, MODE_PRIVATE);
		        SharedPreferences.Editor editor = settings.edit();
		        editor.putBoolean("joined", true);					// 다시 가입처리가 안되게 값 설정
		        editor.putString("wardName", nameEt.getText().toString());
		        editor.putString("phone1", phoneEt.getText().toString());
		        editor.putString("phone2", subPhoneEt.getText().toString());
		        editor.putString("wardEmail", wardEmailEt.getText().toString());
		        editor.putString("parentEmail", parentEmailEt.getText().toString());
		        editor.putString("wardImg",  receiveFiles);
		        editor.commit();		// 커밋으로 공유환경설정에 저장

		        // 피보호자화면으로 이동
				Intent intent = new Intent(ChildRegActivity.this,
						WardModeActivity.class);
				startActivity(intent);
				finish();

			} else {
				Toast.makeText(ChildRegActivity.this, "유저 등록 실패!\n 네트워크 상태 및 서버상태를 체크하세요",
						Toast.LENGTH_LONG).show();
				// 전송 실패 처리 해야됨
			}
			// 화면 고정 해제
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}

		/**
		 * @see android.os.AsyncTask#onPreExecute() 파일 전송중 로딩바 나타내기
		 */
		@Override
		protected void onPreExecute() {	// 전송전 프로그래스 다이얼로그로 전송중임을 사용자에게 알린다.
			dialog = ProgressDialog.show(ChildRegActivity.this, "전송중",
					"사용자 환경에 따라 전송 속도가 다를수 있습니다." + " 잠시만 기다려주세요", true);
			 dialog.show();
		}

		@Override
		protected void onProgressUpdate(final String... values) {
		}

		/**
		 * @see android.os.AsyncTask#doInBackground(Params[]) 비동기 모드로 전송
		 */
		@Override
		protected Boolean doInBackground(final JoinParams... params) {	// 전송중
			boolean result = false;	// 결과 처리값
			// 네트워크 상태 체크
			if (!checkNetWork(true)) {
				return false;
			}

			 // ftp 연결상태 체크
			if (!connectFTP()) {
				return false;
			}
			// 입력정보 파라미터
			JoinParams jp = params[0];

			// http 로 보낼 이름 값 쌍 컬랙션
			Vector<NameValuePair> vars = new Vector<NameValuePair>();
			DeviceInfo di = DeviceInfo
					.setDeviceInfo((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));	// 디바이스 정보
			try {
				/* 파일 업로드 */
				String imageFile = jp.fileName.substring(jp.fileName.lastIndexOf("/") + 1); // 실제 파일명만	 가졍옴
				// 서버에 이미지 중복 방지를 위해 이름 바꾸기 yyyymmdd_hhmmss_Cellnum_01.xxx
				String receiveFiles = getDateTime() + "_" + di.getDeviceNumber() +  getExtension(imageFile, ".");

				// 파일 업로드
				if (!mFtp.upload(jp.fileName, receiveFiles)) {
					// 업로드 에러시
					return false;
				} else {
					// this.receiveFiles = receiveFiles;
				}
				// HTTP post 메서드를 이용하여 데이터 업로드 처리
	            vars.add(new BasicNameValuePair("name", jp.name));			// 이름
	            vars.add(new BasicNameValuePair("phone1", jp.phone));			// 주전화번호
	            vars.add(new BasicNameValuePair("phone2", jp.subPhone));			// 보조 전화번호
	            vars.add(new BasicNameValuePair("parent_email", jp.parentEmail));	// 이메일
	            vars.add(new BasicNameValuePair("ward_email", jp.wardEmail));		// 이메일
	            vars.add(new BasicNameValuePair("user_image", receiveFiles));	// 이미지명
	            vars.add(new BasicNameValuePair("child_phone", di.getDeviceNumber()));	// 사용자 전화번호
	            String url = "http://" + SERVER_URL + UPLOAD_URL;	// + "?" + URLEncodedUtils.format(vars, null);
	            HttpPost request = new HttpPost(url);
	            // 한글깨짐을 방지하기 위해 utf-8 로 인코딩시키자
				UrlEncodedFormEntity entity = null;
				entity = new UrlEncodedFormEntity(vars, "UTF-8");
				request.setEntity(entity);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                HttpClient client = new DefaultHttpClient();
                final String responseBody = client.execute(request, responseHandler);	// 전송
                Log.i(DEBUG_TAG, responseBody);
                if (responseBody.trim().contains("ok")) {	// 정상등록
                	Log.i(DEBUG_TAG, responseBody);
    				  result = true;
                }else if (responseBody.contains("fail")) {	// 등록 실패
                	ChildRegActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							Toast.makeText(ChildRegActivity.this, "단말기 정보가 있습니다.", Toast.LENGTH_SHORT).show();
						}
					});
                }else{
                	Log.i(DEBUG_TAG, responseBody);
                }
            } catch (IOException e) {
            	Log.e(DEBUG_TAG, "io error: ", e);
			} catch (Exception e) {
				Log.e(DEBUG_TAG,  "파일 업로드 에러", e);
			}

			return result;
		}

	}

	@Override
	public void onBackPressed() {	//  뒤로 가기버튼 클릭시 종료 여부
		finishDialog(this);

	}

	/**
	 *	회원 가입시 필요한 파라미터 클래스
	 */
	public class JoinParams{
		String name;
		String phone;
		String subPhone;
		String parentEmail;
		String wardEmail;
		String fileName;
	}

}
