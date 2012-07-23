package kr.co.sbh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

/**
 * 보호자 혹은 피보호자 선택 엑티비티
 */
public class MenuActivity extends BaseActivity {
	private SharedPreferences settings;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_layout);
        settings = getSharedPreferences(PREFER, MODE_PRIVATE);
       	this.init();	// 초기화
    }

    /**
     * 레이아웃 설정
     */
    private void init(){
    }

    public void mOnClick(final View v){
    	Intent intent=  null;
    	if(v.getId() == R.id.child_mode){
			 if(settings.getBoolean("joined", false)){
				 intent =  new Intent(this, WardModeActivity.class);
			 }else{
				 intent = new Intent(this, ChildRegActivity.class);
			 }
    	}else if(v.getId() == R.id.parent_mode){
    		// 공유 환경 설정에 저장
    		/*
    		SharedPreferences sp = getSharedPreferences(PREFER, MODE_PRIVATE);
    		SharedPreferences.Editor editor = sp.edit();
    		editor.putBoolean("isParent", true);
    		editor.commit();
    		*/
    		intent = new Intent(this, ParentModeActivity.class);
    	}
    	startActivity(intent);
    	finish();
    }


}