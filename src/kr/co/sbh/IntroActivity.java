package kr.co.sbh;

import java.io.IOException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 *	첫시작 엑티비티
 *	전화정보를 가져오고 터치시 다음화면으로 이동
 */
public class IntroActivity extends BaseActivity {
	private String cellNum = "";
	private TrackerService trackerService;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_layout);
       	this.init();	// 초기화

    }


    /**
     *	초기설정
     *  전화번호 가져오기 및 전화번호 없을시 인증 하기
     */
    private void init(){
        DeviceInfo di = DeviceInfo.setDeviceInfo((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE)) ;
        this.cellNum = di.getDeviceNumber();
        Log.i(DEBUG_TAG, "tel :" + di.getDeviceNumber());
    }

	/**
	 *  다음 화면으로 넘기기
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		// TODO Auto-generated method stub
		  if ( event.getAction() == MotionEvent.ACTION_DOWN ){
			 SharedPreferences sp = getSharedPreferences(PREFER, MODE_PRIVATE);
			 Intent intent;
			 /*
			 if(sp.getBoolean("isSelected", false)){		// 모드 선택 되었으면
				 if(sp.getBoolean("isParent", false)){	// 부모(보호자 모드)인지
					// intent =  new Intent(this, PathActivity.class);
					 intent =  new Intent(this, WardModeActivity.class);
				 }else{	// 아동(피보호자) 모드
					 intent =  new Intent(this, WardModeActivity.class);
				 }
			 }else{
				 // 모드 선택 엑티비티로..
				 intent =  new Intent(this, MenuActivity.class);
			 }
			 */
			 // 모드 선택 엑티비티로..
			 intent =  new Intent(this, MenuActivity.class);
			 startActivity(intent);
			 finish();
			 return true;
		  }

		  return super.onTouchEvent(event);



	}

	private void sendEmail() {
		new sendEmailToServer().start();
	}

	/**
	 * 서버에 저장된 보호자의 정보를 가지고 서버에서 이메일을 발송한다.
	 */
	private class sendEmailToServer extends Thread{

		@Override
		public void run() {
			super.run();
			try {
					HttpGet request = new HttpGet("http://ddononi.cafe24.com/safeComeHome/sendEmail.php");
	                ResponseHandler<String> responseHandler = new BasicResponseHandler();
	                HttpClient client = new DefaultHttpClient();
	                final String responseBody = client.execute(request, responseHandler);	// 전송
	                if (responseBody.trim().contains("ok")) {
	                	Log.i(BaseActivity.DEBUG_TAG, " 이메일 발송");
	                }

	            } catch (ClientProtocolException e) {
	            	Log.e(BaseActivity.DEBUG_TAG, "발송 실패 ", e);
	            } catch (IOException e) {
	            	Log.e(BaseActivity.DEBUG_TAG, "io 에러: ", e);
	            }
		}
	}

}