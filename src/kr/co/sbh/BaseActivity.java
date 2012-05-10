package kr.co.sbh;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * 기본 설정 베이스 액티비티
 *
 */
public class BaseActivity extends Activity {
    /* Debug setting */
    public static final String DEBUG_TAG = "sbh";
    
    /* Server setting */
    public static final String SERVER_URL = "ddononi.cafe24.com/sch/";	//	서버 주소 
    public static final int MAX_SERVER_CONNECT_COUNT = 5;	//	최대 서버 연결 회수
    
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
	
    /*
     * 앱  종료 다이얼로그
     */
    public void finishDialog(Context context){
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle("").setMessage("종료 하시겠습니까?")
		.setPositiveButton("종료", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				moveTaskToBack(true);
				finish();
				android.os.Process.killProcess(android.os.Process.myPid() ); 
			}
		}).setNegativeButton("취소",null).show();
    }
    
    /*
     *	앱 정보 다이얼로그
     */
    public void appInfoDialog(Context context){
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle("").setMessage( APP_NAME + " ver." + PUBLISH_VERSION )
		.setPositiveButton("확인",null).show();
    } 
    
    /*
     * 	도움말 다이얼로그
     */
    public void appHelpDialog(Context context){
    	String str = getResources().getString( R.string.app_help );
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle("도움말").setMessage(str)
		.setPositiveButton("확인",null).show();
    }     
   
}

