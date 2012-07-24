package kr.co.sbh;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

/**
 * 기본 설정 베이스 액티비티
 *
 */
public class BaseActivity extends Activity {
	/* app setting */
	public final static String  PREFER = "sbh_preference";

    /* Debug setting */
    public static final String DEBUG_TAG = "sbh";

    /* Server setting */
    public static final String SERVER_URL = "ddononi.cafe24.com";	//	서버 주소
    public static final int MAX_SERVER_CONNECT_COUNT = 5;	//	최대 서버 연결 회수
    public static final String GET_URL = "/safeComeHome/loadPath.php";	// 경로 가져오기
    public static final String WARD_INFO_URL = "/safeComeHome/loadWardInfo.php";	// 피보호자 정보 가져오기
    public static final String WARD_IMAGE_URL = "/safeComeHome/uploads/";

    public static final int MAX_FILE_NAME_LENGTH = 100;			// 최대 파일 이름
    public static final int MAX_FILE_SIZE = 5242880;			// 최대 사진 파일 전송 사이즈 5 Mb
    public static final int SERVER_FTP_PORT = 21;
   // public static final int SERVER_TCP_PORT = 5379;
    public static final String FTP_NAME = "ddononi";			// ************해당 ftp id 로 수정***********************//
    public static final String FTP_PASSWORD = "goqkfkrl01";		// ************해당 ftp pass 로 수정***********************//
    public static final String UPLOAD_URL = "/safeComeHome/insert.php";	// 유저 등록 url
    public static final String PATH_UPLOAD_URL = "/safeComeHome/upload_latlng.php";	// 경로 업로드 url
    public static final String FTP_PATH = "/www/safeComeHome/uploads/";	//	ftp path
    public static final String EMAIL_SEND_URL = "/safeComeHome/sendEmail.php";

    /* map */
    public static final String MAP_KEY = "15e7687bc114182c8e799ff28d716d48ae81a2ef";
    public static final String DAUM_LOCAL_KEY = "1a4150ac00469d2392fab7b8c0ff9b076dc07ad1";
    public static final int START_TAG = 1;
    public static final int END_TAG = 2;
    public static final int ACCURATE_VALUE = 200;
    /**
     * 안드로이드에서 제공하는 가이드에 따라 해당 위치정보에 대한 최소한의 신뢰성을 보장하기 위해
     * 정보간의 시간차는 2분 이상 경과로 한다.
     */
    public static final int TWO_MINUTES = 1000 * 60 * 2;

	// 메뉴
	public static final int VIEW_MAP = 1;
	public static final int INFO_VIEW = 2;
	public static final int STANDARD = 3;
	public static final int SATELLITE = 4;
	public static final int HYBRID = 5;

	// msg
	public static final int OK = 1;
    public final static int API_RESULT_OK = 200;	// 다음 api 키 인증 성공값

    /* Preferences setting */
    public static final String PREFERENCE = "safeBackHomePrefs";
    public static final String APP_VERSION = "1.0";
    public static final String PUBLISH_VERSION = "1.00d";
    public static final String APP_NAME = "안심귀가 앱";
    public static final String VERION_ID = "SBH.1.0";
    public static final int ACTION_RESULT = 0;
    public static final String RECEIVE_OK = "ok";	//
    public static final int MAX_TIME_LIMIT = 2000;	//	최대 연결 타임

    /** notif id 값 */
	public static final int MY_NOTIFICATION_ID = 1;	// 앱 아이디값
	/** 미이동 주기 카운트 */
	public static final int CALL_COUNT = 10;


	public static Typeface typeFace = null;
	public static Typeface typeFaceBold = null;
	public final static Calendar calendar = Calendar.getInstance();

	// 아웃풋 파일 경로
	public static final String OUTPUT_PATH = "/sdcard/";
	public static final String FILENAME = "sch.mp4";
	public static final String OUTPUT_FILE = OUTPUT_PATH + FILENAME;

	// 이메일 발송 메세지
	public static final String EMAIL_MSG = "피보호자가 위험합니다!! 피보호자의 신변을 확인하세요";


	// rec end time
	public static final int REC_TIME = 1000 * 30;	// 30sec

	public static final String ACTION_SENT = "kr.co.sbh.SENT";
	public static final String ACTION_DELIVERED = "kr.co.sbh.DELIVERED";

    /*
     * 앱  종료 다이얼로그
     */
    public void finishDialog(final Context context){
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle("").setMessage("종료 하시겠습니까?")
		.setPositiveButton("종료", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				// TODO Auto-generated method stub
				// 서비스중지
				Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
				stopService(serviceIntent);
				Log.i(DEBUG_TAG, "service start!!");

				moveTaskToBack(true);
				finish();
				android.os.Process.killProcess(android.os.Process.myPid() );
			}
		}).setNegativeButton("취소",null).show();
    }

    /*
     *	앱 정보 다이얼로그
     */
    public void appInfoDialog(final Context context){
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle("").setMessage( APP_NAME + " ver." + PUBLISH_VERSION )
		.setPositiveButton("확인",null).show();
    }

    /*
     * 	도움말 다이얼로그
     */
    public void appHelpDialog(final Context context){
    	String str = getResources().getString( R.string.app_help );
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle("도움말").setMessage(str)
		.setPositiveButton("확인",null).show();
    }

	/**
	 * 네트워크망을 사용가능한지 혹은 연결되어있는지 확인한다.
	 * msgFlag가 false이면 현재 연결되어 있는 네트워크를 알려준다.
	 * 네트워크망 연결 불가시 사용자 에게 다이얼로그창을 띄어 알린다.
	 * @param msgFlag
	 * 		Toast 메세지  사용여부
	 * @return
	 *		네트워크 사용가능 여부
	 */
	public boolean checkNetWork(final boolean msgFlag) {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		// boolean isWifiAvail = ni.isAvailable();
		boolean isWifiConn = ni.isConnectedOrConnecting();
		ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		// boolean isMobileAvail = ni.isAvailable();
		boolean isMobileConn = ni.isConnectedOrConnecting();
		if (isWifiConn) {
			if (msgFlag == false) {
				Toast.makeText(this, "Wi-Fi망에 접속중입니다.",
						Toast.LENGTH_SHORT).show();
			}
		} else {
			if (msgFlag == false) {
				Toast.makeText(this, "3G망에 접속중입니다.",
						Toast.LENGTH_SHORT).show();
			}
		}

		if (!isMobileConn && !isWifiConn) {
			/*
			 * 네트워크 연결이 되지 않을경우 이전 화면으로 돌아간다.
			 */
			new AlertDialog.Builder(this)
			.setTitle("알림")
			.setMessage(
					"Wifi 혹은 3G망이 연결되지 않았거나 "
							+ "원활하지 않습니다.네트워크 확인후 다시 접속해 주세요!")
			.setPositiveButton("닫기",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							dialog.dismiss(); // 닫기
							finish();
						}
					}).show();
			return false;
		}
		return true;

	}

}

