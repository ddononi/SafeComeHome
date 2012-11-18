package kr.co.sbh;

import java.io.IOException;
import java.util.Vector;

import net.daum.mf.map.api.MapPoint;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class TraceService extends Service implements LocationListener {
	private Location mLocation;
	private SharedPreferences settings;
	private SharedPreferences mPrefer;
	private UploadToServer uts; // 서버 업로드 쓰레드

	/** 서비스가 실행될때 */
	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		// 피보호자가입을 했다면 피보호자로 판단
		settings = getSharedPreferences(BaseActivity.PREFER, MODE_PRIVATE);
		mPrefer = getSharedPreferences("sbh", MODE_PRIVATE);
		if (settings.getBoolean("joined", false) == false) {
			stopSelf();
			Log.i("sch", "stopself service~");
			return 0;
		}
		startTracker();
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.i("dservice", "stop!");
		// 서비스가 끝날때 위치 수신리스너 제거
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(this);

		super.onDestroy();
	}

	public void startTracker() {
		getLocation();
	}

	/**
	 * 위치 리스너 설정
	 */
	private void getLocation() {

		LocationManager locationManager;
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// criteria 를 이용하여 최적의 위치정보 제공자를 찾는다.
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);// 정확도
		criteria.setPowerRequirement(Criteria.POWER_HIGH); // 전원 소비량
		criteria.setAltitudeRequired(false); // 고도 사용여부
		criteria.setBearingRequired(false); //
		criteria.setSpeedRequired(false); // 속도
		criteria.setCostAllowed(true); // 금전적비용

		String provider = locationManager.getBestProvider(criteria, true);
		// 1분 간격
		locationManager.requestLocationUpdates(provider, 10000L, 100, this);
		mLocation = locationManager.getLastKnownLocation(provider);
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.w(BaseActivity.DEBUG_TAG, "onLocationChanged");
		if (mPrefer.getBoolean("isStarted", false)) {
			TraceService.this.stopSelf();
		}
		MapPoint point = MapPoint.mapPointWithGeoCoord(location.getLatitude(),
				location.getLongitude());
		uts = new UploadToServer(point, "");
		uts.start();
		mLocation = location; // 위치 받아오기

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	/**
	 * 경로 좌표를 서버에 전송한다.
	 */
	private class UploadToServer extends Thread {
		private MapPoint point;
		private String flag;

		public UploadToServer(MapPoint point, String flag) {
			this.point = point;
			this.flag = flag;
		}

		@Override
		public void run() {
			super.run();
			// http 로 보낼 이름 값 쌍 컬랙션
			Vector<NameValuePair> vars = new Vector<NameValuePair>();

			try {
				// HTTP post 메서드를 이용하여 데이터 업로드 처리
				// 위경도 좌표를 서버에 전송
				vars.add(new BasicNameValuePair("lat", String.valueOf(point
						.getMapPointGeoCoord().latitude)));
				vars.add(new BasicNameValuePair("lng", String.valueOf(point
						.getMapPointGeoCoord().longitude)));
				vars.add(new BasicNameValuePair("flag", "trace"));
				// vars.add(new BasicNameValuePair("flag",));
				HttpPost request = new HttpPost("http://"
						+ BaseActivity.SERVER_URL
						+ BaseActivity.PATH_UPLOAD_URL);
				// 한글깨짐을 방지하기 위해 utf-8 로 인코딩시키자
				UrlEncodedFormEntity entity = null;
				entity = new UrlEncodedFormEntity(vars, "UTF-8");
				request.setEntity(entity);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				HttpClient client = new DefaultHttpClient();
				final String responseBody = client.execute(request,
						responseHandler); // 전송
				if (responseBody.trim().contains("ok")) {
					Log.i(BaseActivity.DEBUG_TAG, "정상업로드");
				}

			} catch (ClientProtocolException e) {
				Log.e(BaseActivity.DEBUG_TAG,
						"Failed to get playerId (protocol): ", e);
			} catch (IOException e) {
				Log.e(BaseActivity.DEBUG_TAG, "Failed to get playerId (io): ",
						e);
			}
		}
	}

}
