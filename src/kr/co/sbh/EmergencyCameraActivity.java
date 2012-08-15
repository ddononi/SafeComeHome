package kr.co.sbh;

import java.io.File;
import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

public class EmergencyCameraActivity extends BaseActivity implements
		SurfaceHolder.Callback {
	private MyFTPClient mFtp = null;
	private Display display;
	private boolean isRec; // 레코딩 상태여부

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		// on Pause 상태에서 카메라 ,레코더 객체를 정리한다
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
		if (recorder != null) {
			if (isRec) {
				isRec = false;
				recorder.stop();
				recorder.release();
			}
			recorder = null;
		}
		super.onPause();
	}

	// Video View 객체
	private VideoView mVideoView = null;
	// 카메라 객체
	private Camera mCamera;
	// 레코더 객체 생성
	private MediaRecorder recorder = null;
	// 녹화 시간 - 10초
	private static final int RECORDING_TIME = 30000;

	// 카메라 프리뷰를 설정한다
	private void setCameraPreview(final SurfaceHolder holder) {
		try {
			// 카메라 객체를 만든다
			mCamera = Camera.open();
			// 카메라 객체의 파라메터를 얻고 로테이션을 90도 꺽는다,옵Q의 경우 90회전을 필요로 한다 ,옵Q는 지원
			// 안하는듯....
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setRotation(90);
			mCamera.setParameters(parameters);
			// 프리뷰 디스플레이를 담당한 서피스 홀더를 설정한다
			mCamera.setPreviewDisplay(holder);
			// 프리뷰 콜백을 설정한다 - 프레임 설정이 가능하다,
			/*
			 * mCamera.setPreviewCallback(new PreviewCallback() {
			 * 
			 * @Override public void onPreviewFrame(byte[] data, Camera camera)
			 * { // TODO Auto-generated method stub } });
			 */
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void surfaceCreated(final SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// 서피스가 만들어졌을 때의 대응 루틴
		setCameraPreview(holder);
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format,
			final int width, final int height) {

		// TODO Auto-generated method stub
		// 서피스 변경되었을 때의 대응 루틴
		if (mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			// 프리뷰 사이즈 값 재조정
			// parameters.setPreviewSize(width, height);
			// mCamera.setParameters(parameters);
			// 프리뷰 다시 시작
			mCamera.startPreview();
		}
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder holder) {
		// TODO Auto-generated method stub

		// 서피스 소멸시의 대응 루틴

		// 프리뷰를 멈춘다
		if (mCamera != null) {
			mCamera.stopPreview();
			// 카메라 객체 초기화
			mCamera = null;
		}

	}

	// 프리뷰(카메라가 찍고 있는 화상을 보여주는 화면) 설정 함수
	private void setPreview() {
		// 1) 레이아웃의 videoView 를 멤버 변수에 매핑한다
		mVideoView = (VideoView) findViewById(R.id.videoView);
		// 2) surface holder 변수를 만들고 videoView로부터 인스턴스를 얻어온다
		final SurfaceHolder holder = mVideoView.getHolder();
		// 3)표면의 변화를 통지받을 콜백 객체를 등록한다
		holder.addCallback(this);
		// 4)Surface view의 유형을 설정한다, 아래 타입은 버퍼가 없이도 화면을 표시할 때 사용된다.카메라 프리뷰는 별도의
		// 버퍼가 필요없다
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}

	/*
	 * private void setButtons() { // Rec Start 버튼 콜백 설정 Button recStart =
	 * (Button) findViewById(R.id.RecStart); recStart.setOnClickListener(new
	 * View.OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { Log.e("CAM TEST",
	 * "REC START!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	 * 
	 * if (mVideoView.getHolder() == null) { Log.e("CAM TEST",
	 * "View Err!!!!!!!!!!!!!!!"); } beginRecording(mVideoView.getHolder());
	 * 
	 * } });
	 * 
	 * // Rec Stop 버튼 콜백 설정 Button recStop = (Button)
	 * findViewById(R.id.RecStop); recStop.setOnClickListener(new
	 * View.OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { // TODO Auto-generated method
	 * stub // 레코더 객체가 존재할 경우 이를 스톱시킨다 if (recorder != null) { Log.e("CAM TEST",
	 * "CAMERA STOP!!!!!"); recorder.stop(); recorder.release(); recorder =
	 * null; } // 프리뷰가 없을 경우 다시 가동 시킨다 if (mCamera == null) { Log.e("CAM TEST",
	 * "Preview Restart!!!!!"); // 프리뷰 다시 설정
	 * setCameraPreview(mVideoView.getHolder()); // 프리뷰 재시작
	 * mCamera.startPreview(); }
	 * 
	 * } }); }
	 */

	/*
	 * 자동 동영상 촬영이기 때문에 홈키 및 백키를 막는다.
	 */
	@Override
	public void onAttachedToWindow() {
		// 홈키로 가기 막기
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		super.onAttachedToWindow();
	}

	@Override
	public void onBackPressed() {
		// 백키 막기
		return;
	}

	private void beginRecording(final SurfaceHolder holder) {
		// 레코더 객체 초기화
		Log.e("CAM TEST", "#1 Begin REC!!!");
		if (recorder != null) {
			recorder.stop();
			recorder.release();
		}
		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			Log.e("CAM TEST", "I/O Exception");
		}
		// 파일 생성/초기화
		Log.e("CAM TEST", "#2 Create File!!!");
		File outFile = new File(OUTPUT_FILE);
		if (outFile.exists()) {
			outFile.delete();
		}
		Log.e("CAM TEST", "#3 Release Camera!!!");
		if(prepareMediaRecorder(holder)){
			recorder.start();
			isRec = true;
		}
		/*
		 * if (mCamera != null) { mCamera.stopPreview(); mCamera.release();
		 * mCamera = null; Log.e("CAM TEST", "#3 Release Camera  _---> OK!!!");
		 * }
		 */
		/*
		try {
			recorder = new MediaRecorder();
			recorder.setCamera(mCamera);
			// Video/Audio 소스 설정
			recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			// recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
			// Video/Audio 인코더 설정
			recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
			// recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			recorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_LOW));
			// 레코더 사이즈 설정
			recorder.setVideoSize(display.getWidth(), display.getHeight());
			recorder.setVideoFrameRate(25);
			// 프리뷰를 보여줄 서피스 설정
			recorder.setPreviewDisplay(holder.getSurface());
			// 녹화할 대상 파일 설정
			recorder.setOutputFile(OUTPUT_FILE);
			// 녹화 시간 한계 , 10초
			recorder.setMaxDuration(RECORDING_TIME);
			recorder.prepare();
			recorder.start();
			Log.i("emergency", "start record");

			isRec = true;

		} catch (IllegalStateException e) {
			// TODO: handle exception
			Log.e("CAM TEST", "error");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("CAM TEST", "errorrrrr");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("CAM TEST", "errorrrrr");
		}
		*/

	}

	private Camera getCameraInstance() {
		// TODO Auto-generated method stub
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private boolean prepareMediaRecorder(final SurfaceHolder holder){
		recorder = new MediaRecorder();
		
		mCamera.unlock();
		recorder.setCamera(mCamera);

		recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		recorder.setProfile(CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH));

		recorder.setOutputFile("/sdcard/myvideo.mp4");
		recorder.setMaxDuration(10000); // Set max duration 60 sec.
		recorder.setMaxFileSize(5000000); // Set max file size 5M

		recorder.setPreviewDisplay(holder.getSurface());

		try {
			recorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;

	}


	private void releaseMediaRecorder() {
		if (recorder != null) {
			recorder.reset(); // clear recorder configuration
			recorder.release(); // release the recorder object
			recorder = null;
			mCamera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_layout);
		// 윈도우 디스플레이 얻기
		display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		// 프리뷰를 설정한다
		setPreview();

		// 자동으로 촬영 및 종료 선택
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				beginRecording(mVideoView.getHolder());
			}
		}, 2000);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (recorder != null) {
					Log.e("CAM TEST", "CAMERA STOP!!!!!");
					try {
						recorder.stop();
						recorder.release();
					} catch (IllegalStateException ise) {

					} finally {
						recorder = null;
					}
				}

				// 서버에 전송
				new AsyncTaskFileUpload().execute();
			}
		}, REC_TIME);

	}

	/**
	 * 문자 발송하기
	 * 
	 * @param message
	 */
	private void sendSMS(final String message) {
		// PendingIntent pItent = PendingIntent.getBroadcast(this, 0, new
		// Intent(ACTION_SENT), 0);
		android.telephony.SmsManager manager = android.telephony.SmsManager
				.getDefault();
		// 공유설정환경에서 보호자 전화번호를 가져온다.
		SharedPreferences sp = getSharedPreferences(PREFER, MODE_PRIVATE);
		String phone1 = "01066089782"; //sp.getString("phone1", "010");
		String phone2 = sp.getString("phone2", "");
		sp.getString("phone1", "");
		sp.getString("phone2", "");
		// 문자 발송
		manager.sendTextMessage(phone1, null, message, null, null);
		manager.sendTextMessage(phone2, null, message, null, null);
	}

	/*
	 * ftp 연결 설정
	 */
	private boolean connectFTP() {
		// ftp에 연결시도
		mFtp = new MyFTPClient(SERVER_URL, SERVER_FTP_PORT, FTP_NAME,
				FTP_PASSWORD);
		if (!mFtp.connect()) { // 연결 실패시
			return false;
		}

		if (!mFtp.login()) { // 로그인 실패시
			return false;
		}

		mFtp.cd(FTP_PATH); // 경로 바꾸기
		return true;
	}

	protected boolean uploadToServer() {
		if (!connectFTP()) { // ftp 연결이 안되면
			// Toast.makeText(this, "서버에 연결이 실패했습니다.",
			// Toast.LENGTH_SHORT).show();
			return false;
		}

		if (!mFtp.upload("/sdcard/myvideo.mp4", FILENAME)) {
			// Toast.makeText(this, "서버에 연결이 실패했습니다.",
			// Toast.LENGTH_SHORT).show();
			return false;
		} else {

		}

		return true;
	}

	/**
	 * 서버에 동영상 파일 업로드 수행
	 */
	private class AsyncTaskFileUpload extends
			AsyncTask<Object, String, Boolean> {
		ProgressDialog dialog = null;

		@Override
		protected void onPostExecute(final Boolean result) { // 전송 완료후
			// 모든 파일이 전송이 완료되면 다이얼로그를 닫는다.
			dialog.dismiss(); // 프로그레스 다이얼로그 닫기
			if (mFtp.isConnected()) { // 연결이 되어 있으면
				mFtp.logout(); // 로그 아웃
			}
			// 파일 전송 결과를 출력
			if (result) { // 파일 전송이 정상이면
				sendEmail(); // 보호자에게 메일 발송
				// 문자 발송하기
				String msg = "피보호자가 위험합니다!! 피보호자의 신변을 확인하세요";
				sendSMS(msg);
				Intent intent = new Intent(EmergencyCameraActivity.this,
						WardModeActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(EmergencyCameraActivity.this,
						"파일 전송 실패!\n 네트워크 상태 및 서버상태를 체크하세요", Toast.LENGTH_LONG)
						.show();
			}

			if (dialog != null) {
				dialog.dismiss();
			}
			// 현재 액티비티 종료
			finish();
		}

		@Override
		protected void onPreExecute() { // 전송전 프로그래스 다이얼로그로 전송중임을 사용자에게 알린다.
			dialog = ProgressDialog.show(EmergencyCameraActivity.this, "전송중",
					"사용자 환경에 따라 전송 속도가 다를수 있습니다." + " 잠시 기다려주세요", true);
		}

		@Override
		protected void onProgressUpdate(final String... values) {
		}

		@Override
		protected Boolean doInBackground(final Object... params) { // 전송중
			return uploadToServer();
		}

	}

	private void sendEmail() {
		new sendEmailToServer().start();
	}

	/**
	 * 서버에 저장된 보호자의 정보를 가지고 서버에서 이메일을 발송한다.
	 */
	private class sendEmailToServer extends Thread {

		@Override
		public void run() {
			super.run();
			try {
				HttpGet request = new HttpGet("http://" + SERVER_URL
						+ EMAIL_SEND_URL);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				HttpClient client = new DefaultHttpClient();
				final String responseBody = client.execute(request,
						responseHandler); // 전송
				if (responseBody.trim().contains("ok")) {
					Log.i(BaseActivity.DEBUG_TAG, " 이메일 발송 성공");
				}

			} catch (ClientProtocolException e) {
				Log.e(BaseActivity.DEBUG_TAG, "발송 실패 ", e);
			} catch (IOException e) {
				Log.e(BaseActivity.DEBUG_TAG, "io 에러: ", e);
			}
		}
	}

}
