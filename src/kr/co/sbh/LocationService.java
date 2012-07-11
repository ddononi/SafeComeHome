package kr.co.sbh;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * 공유설정에서 알람시간을 가져와 알람을 실행하고 알람시간이 되면 브로드캐스팅을 한다.
 */
public class LocationService extends Service {
	private Calendar calendar = null; // 현재시간
	private AlarmManager am = null; // 알람 서비스
	private Location mLocation;
	private PendingIntent sender; // 알람 notification을 위한 팬딩인텐트
	private int alarmDistance; // 알람 발생 간격 미터
	private UploadToServer uts;	// 서버 업로드 쓰레드 	
	protected int alarmCount = 0;	// 위치 이동여부 주기값 
	/** 서비스가 실행될때 */
	@Override
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		getLocation();
		setNotification();

		return 0;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.i("dservice", "stop!");
		if(mLocation != null){
			MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());		
			uts = new UploadToServer(point, "end");		// 종료임을 알린다.
			uts.start();			
		}
		//	서비스가 끝날때 위치 수신리스너 제거
		if(loclistener != null){
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);			
			locationManager.removeUpdates(loclistener);
		}
		
		NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(BaseActivity.MY_NOTIFICATION_ID);
		stopSelf();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(final Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onUnbind(final Intent intent) {
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}

	// 위치 리스너 처리
	private final LocationListener loclistener = new LocationListener() {
		public void onLocationChanged(Location location) {
			Log.w(BaseActivity.DEBUG_TAG, "onLocationChanged");
			// 현재 위치정보가 없을때 한번만 현재 위치로 중심을 이동한다.
			if(mLocation == null){
				Toast.makeText(getApplicationContext(), "현재위치로 이동합니다.", Toast.LENGTH_SHORT).show();
				MapPoint point =  MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude());
				// 경로 그리기 시작
				WardModeActivity.pathList.add(point);	// 경로를 저장한다.							
				uts = new UploadToServer(point, "");
				uts.start();
			}else{
				// 출발후면
				// 이전 위치값과 비교해 정확한 위치라면 list에 넣어준다.
				if(WardModeActivity.isStarted && GeoUtils.isBetterLocation(mLocation,location )){		
					//if(GeoUtils.distanceKm(mLocation.getLatitude(), mLocation.getLongitude(),
					//		location.getLatitude(), location.getLongitude())  >= 0.01){	//10 미터이상 이동했을경우만 패스 저장
						MapPoint point =  MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude());
						WardModeActivity.pathList.add(point);	// 경로를 저장한다.						
						// 서버에 업로드
						uts = new UploadToServer(point, "");
						uts.start();	
						
						alarmCount = 0;
					//}
				}else{
					
					// 일정주기동안 움직임이 없을경우 보호자에게 전화를 건다.

					if(alarmCount++ == BaseActivity.CALL_COUNT){
						// 위급 상황으로 판단하여 자동 동영상 녹화 및 저장후 
						// 서버에 전송한후 서버에서 보호자에게 동영상을 첨부한 이메일을 발송한다.
						Intent intent = new Intent(getBaseContext(), EmergencyCameraActivity.class);
						PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);					
						try {
							pi.send();
						} catch (CanceledException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
			}
			Log.i(BaseActivity.DEBUG_TAG, "위치이동~~!~!");
			mLocation = location;	// 위치 받아오기
		}

		public void onProviderDisabled(final String provider) {
			Log.w(BaseActivity.DEBUG_TAG, "onProviderDisabled");
		}

		public void onProviderEnabled(final String provider) {
			Log.w(BaseActivity.DEBUG_TAG, "onProviderEnabled");
		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
			Log.w(BaseActivity.DEBUG_TAG, "onStatusChanged");
		}
	};

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
		locationManager.requestLocationUpdates(provider, 1000 * 60, 0,
				loclistener);
		mLocation = locationManager.getLastKnownLocation(provider);
		/*
		 * String provider; // gps 가 켜져 있으면 gps로 먼저 수신 if
		 * (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
		 * provider = LocationManager.GPS_PROVIDER;
		 * locationManager.requestLocationUpdates(provider, 0, 0,
		 * loclistener);// 현재정보를 업데이트 mLocation =
		 * locationManager.getLastKnownLocation(provider); } else { // 없으면 null
		 * mLocation = null; }
		 * 
		 * if (mLocation == null) { // 무선 네크워트를 통한 위치 설정이 안되어 있으면 그냥 null 처리 if
		 * (!(locationManager
		 * .isProviderEnabled(LocationManager.NETWORK_PROVIDER))) { }
		 * 
		 * // 네트워크로 위치를 가져옴 provider = LocationManager.NETWORK_PROVIDER; //
		 * criteria.setAccuracy(Criteria.ACCURACY_COARSE); // provider =
		 * locationManager.getBestProvider(criteria, true); mLocation =
		 * locationManager.getLastKnownLocation(provider);
		 * locationManager.requestLocationUpdates(provider, 0, 0, loclistener);
		 * /* Toast.makeText(this, "실내에 있거나 GPS를 이용할수 없어  네트워크를 통해 현재위치를 찾습니다.",
		 * Toast.LENGTH_SHORT).show();
		 */

		// }

	}
	
	
	/**
	 * 일정주기동안 위치 이동이 없으면 보호자에게 전화걸기
	 */
	private void checkMovePath(){
		
	}	
	
	private void setNotification(){
		// MyScheduleActivity 로 엑티비티 설정

		Intent contentIntent = new Intent(this, WardModeActivity.class);
		contentIntent.putExtra("resume", true);
		// 알림클릭시 이동할 엑티비티 설정
		PendingIntent theappIntent = PendingIntent.getActivity(this, 0,contentIntent, 0);
		CharSequence title = "안심귀가앱"; 	// 알림 타이틀
		CharSequence message = "안심 귀가 앱 출발 이동중입니다.";	 // 알림 내용

		Notification notif = new Notification(R.drawable.icon, null,
				System.currentTimeMillis());
		

		notif.flags |= Notification.FLAG_AUTO_CANCEL;	// 클릭시 사라지게		
		notif.setLatestEventInfo(this, title, message, theappIntent);	// 통지바 설정
		NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(BaseActivity.MY_NOTIFICATION_ID, notif);	// 통지하기
	}
			
	
	/**
	 * 경로 좌표를 서버에 전송한다.
	 */
	private class UploadToServer extends Thread{
		private MapPoint point;
		private String flag;
		public UploadToServer(MapPoint point, String flag){
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
	            vars.add(new BasicNameValuePair("lat", String.valueOf(point.getMapPointGeoCoord().latitude)));			
	            vars.add(new BasicNameValuePair("lng", String.valueOf(point.getMapPointGeoCoord().longitude)));	
	            vars.add(new BasicNameValuePair("flag", flag));	
	            //  vars.add(new BasicNameValuePair("flag",));	
	            HttpPost request = new HttpPost("http://" + BaseActivity.SERVER_URL + BaseActivity.PATH_UPLOAD_URL);
	           // 한글깨짐을 방지하기 위해 utf-8 로 인코딩시키자
				UrlEncodedFormEntity entity = null;
				entity = new UrlEncodedFormEntity(vars, "UTF-8");
				request.setEntity(entity);
	                ResponseHandler<String> responseHandler = new BasicResponseHandler();
	                HttpClient client = new DefaultHttpClient();
	                final String responseBody = client.execute(request, responseHandler);	// 전송
	                if (responseBody.trim().contains("ok")) {
	                	Log.i(BaseActivity.DEBUG_TAG, "정상업로드");
	                }

	            } catch (ClientProtocolException e) {
	            	Log.e(BaseActivity.DEBUG_TAG, "Failed to get playerId (protocol): ", e);
	            } catch (IOException e) {
	            	Log.e(BaseActivity.DEBUG_TAG, "Failed to get playerId (io): ", e);
	            }
		}
	}	
	
	public static void email(Context context, String emailTo, String emailCC,
		    String subject, String emailText, List<String> filePaths){
		    //need to "send multiple" to get more than one attachment
		    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
		    emailIntent.setType("plain/text");
		    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, 
		        new String[]{emailTo});
		    emailIntent.putExtra(android.content.Intent.EXTRA_CC, 
		        new String[]{emailCC});
		    //has to be an ArrayList
		    ArrayList<Uri> uris = new ArrayList<Uri>();
		    //convert from paths to Android friendly Parcelable Uri's
		    for (String file : filePaths)
		    {
		        File fileIn = new File(file);
		        Uri u = Uri.fromFile(fileIn);
		        uris.add(u);
		    }
		    emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		    context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
		}

}
