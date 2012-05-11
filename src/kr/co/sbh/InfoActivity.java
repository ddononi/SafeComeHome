package kr.co.sbh;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 *	피보호자정보를 서버로부터 받아와서
 *	엘리먼트 항목에 채워준다.
 */
public class InfoActivity extends BaseActivity {
	private final ProgressDialog dialog = null;
	// ui 처리를 위한 핸들러
	private final Handler handler = new Handler() {
    	@Override
		public void handleMessage(final Message msg) {
    	}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_layout);
		initLayout();
	}

	private void initLayout(){

	}

}
