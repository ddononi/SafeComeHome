package kr.co.sbh;

import java.io.IOException;
import java.util.Vector;

import net.daum.mf.map.api.MapPoint;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;

/**
 *	첫시작 엑티비티 
 *	전화정보를 가져오고 터치시 다음화면으로 이동
 */
public class IntroActivity extends BaseActivity {
	String cellNum = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
	public boolean onTouchEvent(MotionEvent event) {
			

		// TODO Auto-generated method stub
		  if ( event.getAction() == MotionEvent.ACTION_DOWN ){
			 SharedPreferences sp = getSharedPreferences(PREFER, MODE_PRIVATE);
			 Intent intent;
			 if(sp.getBoolean("isSelected", false)){		// 모드 선택 되었으면
				 if(sp.getBoolean("isParent", false)){	// 부모(보호자 모드)인지
					 intent =  new Intent(this, PathActivity.class);
				 }else{	// 아동(피보호자) 모드
					 intent =  new Intent(this, MapActivity.class);
				 }
			 }else{
				 // 모드 선택 엑티비티로..
				 intent =  new Intent(this, MenuActivity.class);
			 }
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