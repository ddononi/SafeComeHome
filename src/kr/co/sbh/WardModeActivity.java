package kr.co.sbh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.ObjectOutputStream.PutField;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import kr.co.sbh.data.PathPoint;
import kr.co.sbh.data.PathWapperData;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPOIItem.MarkerType;
import net.daum.mf.map.api.MapPOIItem.ShowAnimationType;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPoint.GeoCoordinate;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapReverseGeoCoder.ReverseGeoCodingResultListener;
import net.daum.mf.map.api.MapView;
import net.daum.mf.map.api.MapView.CurrentLocationEventListener;
import net.daum.mf.map.api.MapView.MapViewEventListener;
import net.daum.mf.map.api.MapView.OpenAPIKeyAuthenticationResultListener;
import net.daum.mf.map.api.MapView.POIItemEventListener;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 메인 지도 엑티비티
 * 경로를 추적하고 추적된 경로를 서버로 전송되어 디비에 저장된다.
 * 추적이 완료되면 전송을 중지하고 결과 화면으로 넘긴다.
 */
public class WardModeActivity extends BaseActivity implements OpenAPIKeyAuthenticationResultListener, MapViewEventListener,
CurrentLocationEventListener, POIItemEventListener, OnClickListener, ReverseGeoCodingResultListener{
	private MapView mMapView;
	private MapPOIItem poiItem;
	private MapPolyline polyline;
	private MapReverseGeoCoder reverseGeoCoder = null;
	private final MapPolyline tracePath = new MapPolyline();
	private SharedPreferences sp;								// 공유 환경설정
	private UploadToServer uts;	// 서버 업로드 쓰레드
	private TrackerService trackerService;

	private LocationManager locationManager;
	private Location mLocation = null;
	private ArrayList<MapPoint> pathList = new ArrayList<MapPoint>();	// 경로를 저장할 collection list
	private Button traceBtn;
	protected static boolean isStarted = false;	// 출발여부
	protected static boolean isEnded = false;	// 도착여부

	private TextView startPlaceTv;		// 시작 위치주소
	private TextView endPlaceTv;		// 도착 위치주소
	private TextView startTimeTv;		// 출발 시간
	private TextView endTimeTv;			// 도착 시간
	
	private boolean running = false;
	private SharedPreferences mPrefer;
	// ui 처리를 위한 핸들러
	private final Handler mHandler = new Handler() {
    	@Override
		public void handleMessage(final Message msg) {
    		@SuppressWarnings("unchecked")
    		// 핸들러를 통해 path를 그려준다.
			List<PathPoint> list = (List<PathPoint>)msg.obj;
    		updateTrackerPath(list);
    	}
	};	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ward_mode_layout);
        sp = getSharedPreferences(PREFER, MODE_PRIVATE);
        getLocation();
        initMap();
        
        initLayout();
        mPrefer = getSharedPreferences("sbh", MODE_PRIVATE);
		// 서비스 바인딩 시작
		// 서비스설정
		//Intent serviceIntent = new Intent(this, TrackerService.class);        
		//bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
		// 출발후이면 서버에서 이동 경로 가져오기
		if(mPrefer.getBoolean("isStarted", false)){
			running = true;
			new LoadPathThread().start();
			// 다시 서비스 시작
			Intent serviceIntent = new Intent(this, TrackerService.class);        
			startService(serviceIntent);
		}        
    }

	/**
	 * 위치 추적 서비스연결 커낵션
	 */
    /*
	private ServiceConnection serviceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			trackerService = ((TrackerService.TrackerBinder)service).getService();
			updateState();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			trackerService = null;
		}
	};    
	*/

	private void initLayout() {
		traceBtn = (Button)findViewById(R.id.trace_btn);
        traceBtn.setClickable(false);	// 현재 위치를 찾기 전까지는 클릭 잠금

        // 긴급 녹화 발송버튼
        ImageButton emerBtn = (ImageButton)findViewById(R.id.rec_btn);
        //	 내위치 찾기 버튼
        ImageButton myLocBtn = (ImageButton)findViewById(R.id.loc_btn);
        //  이메일 버튼
        ImageButton emailBtn = (ImageButton)findViewById(R.id.email_btn);
        // 바로 전화 걸기 버튼
        ImageButton callBtn = (ImageButton)findViewById(R.id.call_btn);

        emerBtn.setOnClickListener(this);
        myLocBtn.setOnClickListener(this);
        emailBtn.setOnClickListener(this);
        callBtn.setOnClickListener(this);

        // 출발 주소
		startPlaceTv = (TextView)findViewById(R.id.start_place);
		// 도착 주소
		endPlaceTv = (TextView)findViewById(R.id.end_place);

		startTimeTv  = (TextView)findViewById(R.id.startTime);
		endTimeTv  = (TextView)findViewById(R.id.endTime);
	}

	/**
	 * 맵 키 설정 및 초기화 설정
	 */
	private void initMap() {
        mMapView = new MapView(this);
        mMapView.setDaumMapApiKey(MAP_KEY);
        mMapView.setOpenAPIKeyAuthenticationResultListener(this);
        mMapView.setMapViewEventListener(this);
        mMapView.setCurrentLocationEventListener(this);
        mMapView.setPOIItemEventListener(this);
        // 지도 타입 설정
        mMapView.setMapType(MapView.MapType.Standard);
        //	현위치를 표시하는 아이콘(마커)를 화면에 표시 안함
        mMapView.setShowCurrentLocationMarker(false);
        // 경로를 그려줄 색 설정
        tracePath.setLineColor(Color.argb(128, 255, 51, 0));
        ViewGroup parent = (ViewGroup)findViewById(R.id.map_parent);
        parent.addView(mMapView);
	}
	
	/**
	 * 경로가 있는지 체크
	 */
	private void updateState(){
		    ArrayList<PathPoint> list = trackerService.getPathList();
		    if(list != null){
		    	updateTrackerPath(list);
		    }
		    
	}

	/**
	 * 출발버튼을 누르면 위치 서비스를 시작한다.
	 * 위치서버스에서 주기적으로 서버에 좌표를 전송하여
	 * 보호자모드에 경로를 탐색할수 있도록 한다.
	 */
	private void doStartService() {
			// 서비스설정
			Intent serviceIntent = new Intent(this, TrackerService.class);
			stopService(serviceIntent);
			/*
			ArrayList<PathPoint> list = new ArrayList<PathPoint>();
			PathPoint data;
			for(MapPoint point : pathList){
				 GeoCoordinate coord = point.getMapPointGeoCoord();
				 data = new PathPoint();
				 data.setLatitude( coord.latitude);
				 data.setLongitude(coord.longitude);
				 list.add(data);
			}
			serviceIntent.putParcelableArrayListExtra("pathList", list);
			*/
			// 서비스 구동
			startService(serviceIntent);
			// 서비스 바인딩 시작
			//bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			Log.i(DEBUG_TAG, "service start!!");
	}
	

	/*
	 * 위치 설정 초기화
	 */
	private void getLocation() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// 적절한 위치기반공급자를 이용하여 리스너를 설정한다.
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);// 정확도
		criteria.setPowerRequirement(Criteria.POWER_HIGH); // 전원 소비량
		criteria.setAltitudeRequired(false); // 고도 사용여부
		criteria.setBearingRequired(false); //
		criteria.setSpeedRequired(true); // 속도
		criteria.setCostAllowed(true); // 금전적비용

		String provider = locationManager.getBestProvider(criteria, true);
		// 1분 이상 10미터 이상
		locationManager.requestLocationUpdates(provider, 1000 * 60, 0, loclistener);
		mLocation = locationManager.getLastKnownLocation(provider);
	}



	// 위치 리스너 처리 이벤트 처리
	private final LocationListener loclistener = new LocationListener() {
		@Override
		public void onLocationChanged(final Location location) {
			Log.w(DEBUG_TAG, "onLocationChanged");
			// 현재 위치정보가 없을때 한번만 현재 위치로 중심을 이동한다.
			if(mLocation == null){
				return;
			}

			MapPoint point =  MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude());
			mMapView.setMapCenterPoint(point, true);
			// 추적 버튼 풀기
			traceBtn.setClickable(true);
			// 이벤트 리스너 달아주기
	        traceBtn.setOnClickListener(WardModeActivity.this);

			mLocation = location;	// 위치 받아오기
		}

		@Override
		public void onProviderDisabled(final String provider) {
			Log.w(DEBUG_TAG, "onProviderDisabled");
		}

		@Override
		public void onProviderEnabled(final String provider) {
			Log.w(DEBUG_TAG, "onProviderEnabled");
		}

		@Override
		public void onStatusChanged(final String provider, final int status, final Bundle extras) {
			Log.w(DEBUG_TAG, "onStatusChanged");
		}
	};

	@Override
	public void onCalloutBalloonOfPOIItemTouched(final MapView arg0, final MapPOIItem arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDraggablePOIItemMoved(final MapView arg0, final MapPOIItem arg1,
			final MapPoint arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPOIItemSelected(final MapView arg0, final MapPOIItem arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCurrentLocationDeviceHeadingUpdate(final MapView arg0, final float arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCurrentLocationUpdate(final MapView arg0, final MapPoint arg1, final float arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCurrentLocationUpdateCancelled(final MapView arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCurrentLocationUpdateFailed(final MapView arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMapViewCenterPointMoved(final MapView arg0, final MapPoint arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMapViewDoubleTapped(final MapView arg0, final MapPoint arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMapViewLongPressed(final MapView arg0, final MapPoint arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMapViewSingleTapped(final MapView arg0, final MapPoint arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMapViewZoomLevelChanged(final MapView arg0, final int arg1) {
		// TODO Auto-generated method stub

	}

	/**
	 * 키 인증 처리
	 */
	@Override
	public void onDaumMapOpenAPIKeyAuthenticationResult(final MapView v, final int arg1,
			final String arg2) {
	}

	private class CurrentPointThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			while(true){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(mLocation != null){
					MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());
					mMapView.setMapCenterPoint(point, true);
					break;
				}
			}
		}

	}


	/**
	 * list에 저장된 좌표를 이용하여 경로를 그려준다.
	 */
	private void drawPath() {
		// 오전 모든 경로를 지운후 다시 그려준다.
		mMapView.removeAllPolylines();
		for(MapPoint p: pathList){
			tracePath.addPoint(p);
		}
		// 맵뷰에 붙여준다.
		mMapView.addPolyline(tracePath);

	}

	/**
	 * 위치정보의 보다 정확한 수신을 위해
	 * 이전 위치값과 비교한다.
	 * @param location
	 * @param currentBestLocation
	 * @return
	 */
	protected boolean isBetterLocation(final Location location, final Location currentBestLocation){
		if(currentBestLocation == null){
			// 잘못된 위치라도 없는것보다 좋다.
			return true;
		}

		// 기존 정보와 새로운 정보간의 시간 차이를 츨정한다.
		// 2분 이상 경과시 새로운 장소로 이동하였을 가능성이 크므로
		// 해당 정보를 채택하게 된다.
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;	// 새로운 위치정보여부
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;
		// 2분 이상 경가된 위치라면 true otherwise false
		if(isSignificantlyNewer){
			return true;
		}else if(isSignificantlyOlder){
			return false;
		}

		// 위치 정확성 차이
		// 위치정보사업자가 수 미터 거리 내로 정밀한 정보를 제공한다면
		// 그 정보는 정확하다고 판단할 수 있다 여기서는 200미터로 두었다.
		int accuracyDelta = (int)(location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isLMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > ACCURATE_VALUE;

		// 앞에서와같이 상태 변화에 따라 위치정보사업자가 변동될 수 있다.
		// 같은 사업자라 한다면 더욱 정확한 위치정보를 제공해 줄 가능성이 높다고 판단할 수 있다.
		boolean isFormSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// 시간과 거리의 차이를 계산하여 위치의 정확도를 결정한다.
		if(isLMoreAccurate){
			return true;
		}else if(isNewer && !isLessAccurate){
			return true;
		}else if(isNewer && !isSignificantlyLessAccurate && isFormSameProvider){
			return true;
		}

		return false;

	}

	/**
	 * 같은 공급자인지 측정
	 * @param provider1
	 * @param provider2
	 * @return
	 */
	private boolean isSameProvider(final String provider1, final String provider2){
		if(provider1 == null){
			return provider2 == null;
		}
		return provider1.equals(provider2);

	}
	
	

	@Override
	protected void onResume() {
		super.onResume();
		/*
		Intent intent = getIntent();
        if(intent.getBooleanExtra("resume", false) == true){
    		if(intent.hasExtra("pathList")){	// 경로가 있으면 가져온다.
    			ArrayList<PathPoint> list = intent.getParcelableArrayListExtra("pathList");		
	        	updateTrackerPath(list);		
	            intent.removeExtra("resume");	
    		}
        }
        */

        
	}

	/**
	 * 서비스로부터 경로 업데이트
	 * @param list
	 */
	private void updateTrackerPath(List <PathPoint> list) {
		pathList = new ArrayList<MapPoint>();
		for(PathPoint point : list){
			 MapPoint mapPoint =MapPoint.mapPointWithGeoCoord(point.getLatitude(), point.getLongitude());
			 pathList.add(mapPoint);
		}		
		
    	int index = 0;
    	// 우선 모든 아이템과 경로를 지워준다.
    	mMapView.removeAllPOIItems();
		mMapView.removeAllPolylines();
		/*
    	for(MapPoint point : pathList){
    		if(index == 0){	// 경로의 첫 시작이면
        		MapPOIItem item = new MapPOIItem();
        		// poi 아이템 설정
        		item.setTag(START_TAG);
        		item.setItemName("출발");
        		item.setMapPoint(point);
        		item.setShowAnimationType(ShowAnimationType.SpringFromGround);
        		item.setMarkerType(MarkerType.CustomImage);
        		item.setCustomImageResourceId(R.drawable.custom_poi_marker_start);
        		item.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(22,0));
        		// 맵에 붙여준다.
        		mMapView.addPOIItem(item);
    		}
    		
			// 경로 넣어주기
   			//tracePath.addPoint(point);
    		index++;
    	}
    	*/
    	
		drawPath(list);
        
		isStarted = true;
		traceBtn.setText("도착");	        			
	}

	@Override
	public void onClick(final View v) {
		Intent intent;
		switch(v.getId()){
		case R.id.trace_btn:	// 추적 버튼을 눌렀을 경우
			if(isStarted == false){
				if(mLocation == null){	// 위치를 찾을수 없을 경우
					Toast.makeText(WardModeActivity.this, "현재 위치를 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
					return;
				}
				isStarted = true;
				startTrace();
				Toast.makeText(this, "백그라운드로 위치경로를 업데이트 합니다.", Toast.LENGTH_SHORT).show();
				doStartService();				
				// 공유 환경 설정에 출발기록을 하여 추후 다시 출발 상태로 가지 않도록 한다.
				SharedPreferences.Editor editor = mPrefer.edit();
				editor.putBoolean("isStarted", true);	// 출발로 저장
				editor.commit();
				
				//trackerService.startTracker();
				((Button)v).setText("도착");
			}else{
				running = false;
				if(mLocation == null){
					return;
				}
				endTrace();
				isEnded = true;
				// 도착완료이면 클릭 버튼을 숨긴다.
				v.setVisibility(View.GONE);
				// 위치 리스너 제거
				SharedPreferences.Editor editor = mPrefer.edit();
				editor.putBoolean("isStarted", false);	// 출발 해제
				editor.commit();				
				locationManager.removeUpdates(loclistener);
			}
			break;
		case R.id.loc_btn :	// 현재 위치 찾기
			if(mLocation != null){	// 현재 위치 정보가 있을때만
				MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());
				mMapView.setMapCenterPoint(point, true);
			}else{
				// 네트워크로 공급자 변경후 다시 현재 위치 검색
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0, loclistener);

				searchMyPlace();
				Toast.makeText(this, "현재 위치를 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.rec_btn : 	// 긴급 촬영 모드
			// 서버에 전송한후 서버에서 보호자에게 동영상을 첨부한 이메일을 발송한다.
			intent = new Intent(this, EmergencyCameraActivity.class);
			startActivity(intent);
			break;
		case R.id.email_btn :	// 이메일 발송
			// SMS 발송
			String emailAddr = "smsto:" +  sp.getString("phone1", "112");
			Uri uri = Uri.parse(emailAddr);
			intent = new Intent(Intent.ACTION_SENDTO, uri);
			intent.putExtra("sms_body", EMAIL_MSG);
			startActivity(intent);
			break;
		case R.id.call_btn : 		// 보호자에게 전화하기
			// 보호자에게 전화걸기를 시도
			// 전화번호가 없을경우 112로 신고
			uri = Uri.parse("tel:" + sp.getString("phone1", "112") );
			intent = new Intent(Intent.ACTION_CALL,uri);
			startActivity(intent);
			break;
		}
	}
	
	
	

	@Override
	protected void onDestroy() {
		//	도착 혹은 출발하지 않았으면 서비스 및 바인딩 종료
		if(isEnded == true || isStarted == false){
			Intent serviceIntent = new Intent(this, TrackerService.class);
			stopService(serviceIntent);
			// 서비스 바인딩 해제
			trackerService.removePathList();
			//unbindService(serviceConnection);
		}
		running = false;
		super.onDestroy();
		
	}



	private void searchMyPlace() {
		Location tmpLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		// 마지막 위치값이 없으면
		if(tmpLocation == null){
			// 오래된 위치값이면 위치값을 받아 올수 있게 1초를 기다린후 다시 위치 수신 실행
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					searchMyPlace();	// 다시 현재 위치를 찾는다.
				}
			}, 1000);
			return;
		}


		// 이전 시간 비교
		long locationTime = System.currentTimeMillis() - tmpLocation.getTime();
		// 이전 위치값시간이 5분 이내면 위치값 사용
		if(locationTime < (1000 * 60 * 5) ){
			mLocation = tmpLocation;
	        //	 내위치 찾기 버튼
	        ImageButton myLocBtn = (ImageButton)findViewById(R.id.loc_btn);
	        myLocBtn.performClick();
			Geocoder gc = new Geocoder(this,Locale.getDefault());
			List<Address> addresses;
			try {
				addresses = gc.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
				String addressStr = "현위치";
				if(addresses.size()>0) {	// 주소가 있으면
					// 첫번째 주소 컬렉션을 얻은후
					Address address = addresses.get(0);
					// 실제 주소만 가져온다.
					addressStr = address.getAddressLine(0).replace("대한민국", "").trim();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}else{
			// 오래된 위치값이면 위치값을 받아 올수 있게 1초를 기다린후 다시 위치 수신 실행
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					searchMyPlace();	// 다시 현재 위치를 찾는다.
				}
			}, 1000);
		}

	}

	/**
	 * 추적 시작시 출발 오버레이 아이템을 생성하고 맵뷰에 븉여준다.
	 */
	private void startTrace() {

		MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());

		reverseGeoCoder = new MapReverseGeoCoder(DAUM_LOCAL_KEY, point, this, this);
		reverseGeoCoder.startFindingAddress();
		pathList.add(point);
		MapPOIItem item = new MapPOIItem();
		// poi 아이템 설정
		item.setTag(START_TAG);
		item.setItemName("출발");
		item.setMapPoint(point);
		item.setShowAnimationType(ShowAnimationType.SpringFromGround);
		item.setMarkerType(MarkerType.CustomImage);
		item.setCustomImageResourceId(R.drawable.custom_poi_marker_start);
		item.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(22,0));
		// 맵에 붙여준다.
		mMapView.addPOIItem(item);

		startTimeTv.setText("출발시간 : " + new SimpleDateFormat("hh시 mm분 ss초").format(new Date()));
		uts = new UploadToServer(point, "start", startPlaceTv.getText().toString(), startTimeTv.getText().toString());
		uts.start();

	}

	/**
	 * 추적 종료시 종료 오버레이 아이템을 생성하고 맵뷰에 븉여준다.
	 */
	private void endTrace() {

		// 현재 위치를 얻고
		isEnded = true;
		MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());

		
		pathList.add(point);

		reverseGeoCoder = new MapReverseGeoCoder(DAUM_LOCAL_KEY, point, this, this);
		reverseGeoCoder.startFindingAddress();

		MapPOIItem item = new MapPOIItem();
		// poi 아이템 설정
		item.setTag(END_TAG);
		item.setItemName("도착");
		item.setMapPoint(point);
		item.setShowAnimationType(ShowAnimationType.SpringFromGround);
		item.setMarkerType(MarkerType.CustomImage);
		item.setCustomImageResourceId(R.drawable.custom_poi_marker_end);
		item.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(22,0));
		// 맵에 붙여준다.
		mMapView.addPOIItem(item);
		if(polyline != null){
		    polyline = new MapPolyline();
			polyline.setTag(1);
			polyline.setLineColor(Color.argb(128, 255, 0, 0));
		}
		try{
			polyline.addPoint(point);
		}catch(Exception e){}
		// 맵뷰에 붙여준다.
		mMapView.addPolyline(polyline);		
		
		// 서비스설정
		Intent serviceIntent = new Intent(this, TrackerService.class);
		// 서비스 종료
		stopService(serviceIntent);
		endTimeTv.setText("도착시간 : " + new SimpleDateFormat("hh시 mm분 ss초").format(new Date()));
		uts = new UploadToServer(point, "end", endPlaceTv.getText().toString(), endTimeTv.getText().toString());
		uts.start();
		
		drawPath();
		// 현재 위치 아이템 제거
		mMapView.removePOIItem(mMapView.findPOIItemByTag(10));
	}


	@Override
	public void onBackPressed() {	//  뒤로 가기버튼 클릭시 종료 여부
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setTitle("").setMessage("종료 하시겠습니까?")
			.setPositiveButton("종료", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					//	도착 혹은 출발하지 않았으면 서비스 및 바인딩 종료
					if(isEnded == true || isStarted == false){
						Intent serviceIntent = new Intent(WardModeActivity.this, TrackerService.class);
						stopService(serviceIntent);
						// 서비스 바인딩 해제
						//unbindService(serviceConnection);
					}
					moveTaskToBack(true);
					finish();
					android.os.Process.killProcess(android.os.Process.myPid() );
				}
			}).setNegativeButton("취소",null).show();
	}




    /**
     * 옵션 메뉴 처리
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu){
    	super.onCreateOptionsMenu(menu);
    	menu.add(0,1,0, "도움말").setIcon(android.R.drawable.ic_menu_help);
    	menu.add(0,2,0, "프로그램정보").setIcon(android.R.drawable.ic_menu_info_details);
    	menu.add(0,3,0, "지도종류").setIcon(android.R.drawable.ic_menu_gallery);
    	menu.add(0,4,0, "나가기").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	//item.setIcon();
    	return true;
    }


    /**
     * 옵션 메뉴 선택에 따라 해당 처리를 해줌
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item){
    	switch(item.getItemId()){
    		case 1:
    			appHelpDialog(this);
    			return true;
    		case 2:
    			appInfoDialog(this);
    			return true;
    		case 3:
    			String[] mapTypeMenuItems = { "일반지도", "위성지도", "하이브리드"};

    			Builder dialog = new AlertDialog.Builder(this);
    			dialog.setTitle("지도 종류선택");
    			dialog.setItems(mapTypeMenuItems, new DialogInterface.OnClickListener() {
    				@Override
					public void onClick(final DialogInterface dialog, final int which) {
    					switch (which) {
    					case 0: // Standard
    						mMapView.setMapType(MapView.MapType.Standard);
    						break;
    					case 1: // Satellite
    						mMapView.setMapType(MapView.MapType.Satellite);
    						break;
    					case 2: // Hybrid
    						mMapView.setMapType(MapView.MapType.Hybrid);
    						break;
    					}
    				}

    			}).show();
    			return true;
    		case 4:
    			finishDialog(this);
    			return true;
    	}
    	return false;
    }


	@Override
	public void onReverseGeoCoderFoundAddress(final MapReverseGeoCoder rGeoCoder, final String addressString) {

		/*
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("DaumMapLibrarySample");
		alertDialog.setMessage(alertMessage);
		alertDialog.setPositiveButton("OK", null);
		alertDialog.show();
		*/
		// 시작 poiitem 을 찾는다.
		if(isEnded == false){
			MapPOIItem item = mMapView.findPOIItemByTag(START_TAG);
			try{
				item.setItemName(addressString);
			}catch(NullPointerException npe){}
			mMapView.addPOIItem(item);
			startPlaceTv.setText("출발 위치 : " + addressString);
		}else{
			MapPOIItem item = mMapView.findPOIItemByTag(END_TAG);
			try{
				item.setItemName(addressString);
			}catch(NullPointerException npe){}			
			mMapView.addPOIItem(item);
			endPlaceTv.setText("도착 위치 : " + addressString);
		}

		reverseGeoCoder = null;
	}


	@Override
	public void onReverseGeoCoderFailedToFindAddress(final MapReverseGeoCoder rGeoCoder) {
		/*
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("DaumMapLibrarySample");
		alertDialog.setMessage("Reverse Geo-Coding Failed");
		alertDialog.setPositiveButton("OK", null);
		alertDialog.show();
		*/
		reverseGeoCoder = null;
	}


	/**
	 * 경로 좌표를 서버에 전송한다.
	 */
	private class UploadToServer extends Thread{
		private final MapPoint point;
		private final String flag;
		private final String place;
		private final String time;
		public UploadToServer(final MapPoint point, final String flag, final String place, final String time){
			this.point = point;
			this.flag = flag;
			this.place = place;
			this.time = time;
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
	            vars.add(new BasicNameValuePair("place", place));
	            vars.add(new BasicNameValuePair("time", time));

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
	

    /**
     *	이동 경로를 가져올 쓰레드
     */
    private class LoadPathThread extends Thread{

		@Override
		public void run() {
			while (running) {			
			List<PathPoint> list;			
				// TODO Auto-generated method stub
				try {
						Log.i("safe", "thread start" );
						list = processXML();
						Message msg = new Message();
						msg.obj = list;
						mHandler.sendMessage(msg);
						Thread.sleep(10 * 1000);
				} catch (XmlPullParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			super.run();
		}

    }
    
	/**
	 *  get방식으로 서버에서 조회값을 보내  서버에서 생성된 xml를 parsing하여
	 *   이동경로 가져온다.
	 *
	 * @return
	 * 	접속된 전체 유저 리스트
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private List<PathPoint> processXML() throws XmlPullParserException, IOException {
	    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	    XmlPullParser parser = factory.newPullParser();
	    InputStreamReader isr = null;
	    BufferedReader br = null;
		List<PathPoint> list = new ArrayList<PathPoint>();
	    // namespace 지원
	    factory.setNamespaceAware(true);
	    // url 설정
	    URL url = new URL("http://" + SERVER_URL + GET_URL);
	    URLConnection conn = url.openConnection();
	    // 연결 시간 설정
	    conn.setConnectTimeout(2000);
	    conn.setDoInput(true);
	    conn.setDoOutput(true);
		try{
		    isr = new InputStreamReader(conn.getInputStream(), "UTF-8");
		    br = new BufferedReader(isr);
		    StringBuilder xml = new StringBuilder();
		    String line = "";
		    while((line = br.readLine()) != null){
		    	xml.append(line);
		    }
		    String decodeXMl = URLDecoder.decode(xml.toString());
			// xml 외의 문자 제거
		    decodeXMl = decodeXMl.substring(decodeXMl.indexOf("<"), decodeXMl.lastIndexOf(">") + 1);
		    parser.setInput(new StringReader(decodeXMl));
			int eventType = -1;
			// 리스트에 담을 좌표 데이터 클래스
			PathPoint point = null;
			Log.i(DEBUG_TAG, decodeXMl);
			// xml 노드를 순회하면서 위경도좌표와 시각을 가져와 리스트에 담는다.
			while(eventType != XmlResourceParser.END_DOCUMENT){	// 문서의 마지막이 아닐때까지
				if(eventType == XmlResourceParser.START_TAG){	// 이벤트가 시작태그면
					String strName = parser.getName();
					if(strName.contains("lat")){				// 위도
						point = new PathPoint();
						point.setLatitude(Double.valueOf(parser.nextText()));
					}else if(strName.equals("lng")){			// 경도
						point.setLongitude(Double.valueOf(parser.nextText()));
					}else if(strName.equals("flag")){			// 출발, 도착 플레그
						point.setPathFlag(parser.nextText());				
					}else if(strName.equals("date")){			// 업로드 시각
						point.setDate(parser.nextText());
						list.add(point);
					}
				}
				eventType = parser.next();	// 다음이벤트로..
			}
		}finally{
			if(isr != null){
				isr.close();
			}

			if(br != null){
				br.close();
			}
		}
		return list;
	}    
	
	/**
	 * 이동경로 그려주기
	 */
	private void drawPath(final List<PathPoint> list){
	    polyline = new MapPolyline();
		mMapView.removeAllPolylines();	// 이전 지적선이 있으면 지워준다.
		mMapView.removeAllPOIItems();	// 이전 아이템이 있으면 삭제
		polyline.setTag(1);
		polyline.setLineColor(Color.argb(128, 255, 0, 0));
		// list에서 좌표를 가져와 polyline에 넣어준다.
		int index = 0;
		for(PathPoint p :list){
			if(index == 0){	// 출발 아이콘
				// 출발 아이콘 처리
        		MapPOIItem item = new MapPOIItem();
        		MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());
        		reverseGeoCoder = new MapReverseGeoCoder(DAUM_LOCAL_KEY, point, this, this);
        		reverseGeoCoder.startFindingAddress();        		
        		// poi 아이템 설정
        		item.setTag(START_TAG);
        		item.setItemName("출발");
        		item.setMapPoint(MapPoint.mapPointWithGeoCoord(p.getLatitude(), p.getLongitude()));
        		item.setShowAnimationType(ShowAnimationType.SpringFromGround);
        		item.setMarkerType(MarkerType.CustomImage);
        		item.setCustomImageResourceId(R.drawable.custom_poi_marker_start);
        		item.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(22,0));
        		// 맵에 붙여준다.
        		mMapView.addPOIItem(item);

        		// 출발 시간 넣어주기
        		TextView startTimeTv  = (TextView)findViewById(R.id.startTime);
        		startTimeTv.setText(Html.fromHtml("<font style='font-weight:bold;'>출발 시간 :</font> " )+ p.getDate());

                // 출발 주소
        		TextView startPlaceTv = (TextView)findViewById(R.id.start_place);
			}else if(index == list.size() -1 && !p.getPathFlag().equals("end")){	// 도착은 아니지만 마지막 위치에 캐릭터 설정
				MapPOIItem endItem = new MapPOIItem();
				// poi 아이템 설정
				endItem.setTag(10);
				endItem.setItemName("현이동위치");
				endItem.setMapPoint(MapPoint.mapPointWithGeoCoord(p.getLatitude(), p.getLongitude()));
				endItem.setShowAnimationType(ShowAnimationType.SpringFromGround);
				endItem.setMarkerType(MarkerType.CustomImage);
				endItem.setCustomImageResourceId(R.drawable.green);
				endItem.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(22,0));
				// 맵에 붙여준다.
				mMapView.addPOIItem(endItem);	
			}
			polyline.addPoint(
					MapPoint.mapPointWithGeoCoord(p.getLatitude(),  p.getLongitude()));

			index++;
		}
		// 맵뷰에 붙여준다.
		mMapView.addPolyline(polyline);
		// 모든 패스가 보이도록화면을 조정한다.
		mMapView.fitMapViewAreaToShowAllPolylines();
	}
	

	private String getAddress(final double lat, final double lng){
		// Geocoder를 이용하여 좌표를 주소로 변환처리
		Geocoder gc = new Geocoder(this,Locale.getDefault());
		List<Address> addresses = null;
		try {
			addresses = gc.getFromLocation(lat, lng,  1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String addressStr = "현위치";
		if(addresses != null && addresses.size()>0) {	// 주소가 있으면
			// 첫번째 주소 컬렉션을 얻은후
			Address address = addresses.get(0);
			// 실제 주소만 가져온다.
			return address.getAddressLine(0).replace("대한민국", "").trim();
		}

		return "";
	}	


}
