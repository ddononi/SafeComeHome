package kr.co.sbh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import kr.co.sbh.data.WardInfoData;
import kr.co.sbh.widget.WebImageView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 피보호자정보를 서버로부터 받아와서 엘리먼트 항목에 채워준다.
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
		new AsyncWardInfo().execute();
	}

	private void initLayout() {
		// WebImageView wardImage = (WebImageView)findViewById(R.id.ward_image);
		// wardImage.setImasgeUrl("http://" + SERVER_URL + WARD_IMAGE_URL);
	}

	private class AsyncWardInfo extends AsyncTask<Void, Void, Boolean> {
		private WardInfoData data;

		@Override
		protected void onPostExecute(final Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			// 정상 파싱 처리시 엘리먼트에 내용을 채워준다.
			if (result && data != null) {
				WebImageView wardImage = (WebImageView) findViewById(R.id.ward_image);
				wardImage.setImasgeUrl("http://" + SERVER_URL + WARD_IMAGE_URL
						+ data.getImageFileName());

				// 이름 채우기
				TextView name = (TextView) findViewById(R.id.ward_name);
				name.setText(data.getName());

				// 전화번호 채우기
				TextView tel = (TextView) findViewById(R.id.tel);
				tel.setText(data.getTel());
				// 전화걸기
				ImageButton callBtn = (ImageButton) findViewById(R.id.call_btn);
				callBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) { // 전화하기
						Uri uri = Uri.parse("tel:" + data.getTel());
						Intent intent = new Intent(Intent.ACTION_DIAL, uri);
						startActivity(intent);
					}
				});
				// 문자 보내기
				ImageButton smsBtn = (ImageButton) findViewById(R.id.sms_btn);
				smsBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) { // 문자 보내기
						String emailAddr = "smsto:" + data.getTel();
						Uri uri = Uri.parse(emailAddr);
						Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
						startActivity(intent);
					}
				});

				// 주소 채우기
				TextView address = (TextView) findViewById(R.id.address);
				address.setText(data.getAddress());

			} else {
				Toast.makeText(InfoActivity.this, "피보호자 정보를 가져오는 실패 했습니다.",
						Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected Boolean doInBackground(final Void... params) {
			try {
				processXML();
				return true;
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}

		/**
		 * 피보호자 정보 xml 파싱 처리
		 * 
		 * @throws XmlPullParserException
		 * @throws IOException
		 */
		private void processXML() throws XmlPullParserException, IOException {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			InputStreamReader isr = null;
			BufferedReader br = null;
			// namespace 지원
			factory.setNamespaceAware(true);
			// url 설정
			URL url = new URL("http://" + SERVER_URL + WARD_INFO_URL);
			URLConnection conn = url.openConnection();
			// 연결 시간 설정
			conn.setConnectTimeout(2000);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			try {
				isr = new InputStreamReader(conn.getInputStream(), "UTF-8");
				br = new BufferedReader(isr);
				StringBuilder xml = new StringBuilder();
				String line = "";
				while ((line = br.readLine()) != null) {
					xml.append(line);
				}
				String decodeXMl = URLDecoder.decode(xml.toString());

				if (TextUtils.isEmpty(decodeXMl)) {
					new Exception();
				}

				// xml 외의 문자 제거
				decodeXMl = decodeXMl.substring(decodeXMl.indexOf("<"),
						decodeXMl.lastIndexOf(">") + 1);
				parser.setInput(new StringReader(decodeXMl));
				int eventType = -1;
				// xml 노드를 순회하면서 위경도좌표와 시각을 가져와 리스트에 담는다.
				while (eventType != XmlResourceParser.END_DOCUMENT) { // 문서의
																		// 마지막이
																		// 아닐때까지
					if (eventType == XmlResourceParser.START_TAG) { // 이벤트가
																	// 시작태그면
						String strName = parser.getName();
						if (strName.contains("name")) { // 이름
							data = new WardInfoData();
							data.setName(parser.nextText());
						} else if (strName.equals("tel")) { // 전화번호
							data.setTel(parser.nextText());
						} else if (strName.equals("address")) { // 주소
							data.setAddress(parser.nextText());
						} else if (strName.equals("email")) { // 이메일
							data.setEmail(parser.nextText());
						} else if (strName.equals("desc")) { // 특징
							data.setDesc(parser.nextText());
						} else if (strName.equals("imageFileName")) { // 이미지파일명
							data.setImageFileName(parser.nextText());
						}
					}
					eventType = parser.next(); // 다음이벤트로..
				}
			} finally {
				if (isr != null) {
					isr.close();
				}

				if (br != null) {
					br.close();
				}
			}
		}
	}

}
