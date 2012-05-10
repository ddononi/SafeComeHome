package kr.co.sbh;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
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
			 Intent intent =  new Intent(this, MapActivity.class);
			 startActivity(intent);
			 finish();
			 return true;
		  }
		  
		  return super.onTouchEvent(event);
	}
}