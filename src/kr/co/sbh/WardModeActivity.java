package kr.co.sbh;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPOIItem.MarkerType;
import net.daum.mf.map.api.MapPOIItem.ShowAnimationType;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapReverseGeoCoder.ReverseGeoCodingResultListener;
import net.daum.mf.map.api.MapView;
import net.daum.mf.map.api.MapView.CurrentLocationEventListener;
import net.daum.mf.map.api.MapView.MapViewEventListener;
import net.daum.mf.map.api.MapView.OpenAPIKeyAuthenticationResultListener;
import net.daum.mf.map.api.MapView.POIItemEventListener;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
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
	private MapView mapView;
	private MapPOIItem poiItem;
	private MapReverseGeoCoder reverseGeoCoder = null;
	private MapPolyline tracePath = new MapPolyline();
	private SharedPreferences sp;								// 공유 환경설정
	private UploadToServer uts;	// 서버 업로드 쓰레드 		
	
	private LocationManager locationManager;
	private Location mLocation = null;
	protected static List<MapPoint> pathList = new ArrayList<MapPoint>();	// 경로를 저장할 collection list
	private Button traceBtn;
	protected static boolean isStarted = false;	// 출발여부
	protected static boolean isEnded = false;	// 도착여부
	
	private TextView startPlaceTv;		// 시작 위치주소
	private TextView endPlaceTv;		// 도착 위치주소
	private TextView startTimeTv;		// 출발 시간
	private TextView endTimeTv;			// 도착 시간
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);
        sp = getSharedPreferences(PREFER, MODE_PRIVATE);
        getLocation();
        initMap();
        
        Intent it = getIntent();
        if(it.getBooleanExtra("resume", false) == true){
        	int index = 0;
        	// 우선 모든 아이템과 경로를 지워준다.
        	mapView.removeAllPOIItems();       
    		mapView.removeAllPolylines();            	
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
	        		mapView.addPOIItem(item);
        		}
       			tracePath.addPoint(point);
        		// 맵뷰에 붙여준다.
        		mapView.addPolyline(tracePath);
        		index++;
        	}
        }
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
        mapView = new MapView(this);
        mapView.setDaumMapApiKey(MAP_KEY);
        mapView.setOpenAPIKeyAuthenticationResultListener(this);
        mapView.setMapViewEventListener(this);
        mapView.setCurrentLocationEventListener(this);
        mapView.setPOIItemEventListener(this);
        // 지도 타입 설정
        mapView.setMapType(MapView.MapType.Standard);
        //	현위치를 표시하는 아이콘(마커)를 화면에 표시 안함
        mapView.setShowCurrentLocationMarker(false);
        // 경로를 그려줄 색 설정
        tracePath.setLineColor(Color.argb(128, 255, 51, 0));		
        ViewGroup parent = (ViewGroup)findViewById(R.id.map_parent);
        parent.addView(mapView);
	}

	/**
	 * 출발버튼을 누르면 위치 서비스를 시작한다.
	 * 위치서버스에서 주기적으로 서버에 좌표를 전송하여 
	 * 보호자모드에 경로를 탐색할수 있도록 한다.
	 */
	private void doStartService() {
			// 서비스설정
			Intent serviceIntent = new Intent(this, LocationService.class);
			stopService(serviceIntent);
			startService(serviceIntent);
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
		public void onLocationChanged(Location location) {
			Log.w(DEBUG_TAG, "onLocationChanged");
			// 현재 위치정보가 없을때 한번만 현재 위치로 중심을 이동한다.
			if(mLocation == null){
				return;
			}
			
			Toast.makeText(WardModeActivity.this, "현재위치로 이동합니다.", Toast.LENGTH_SHORT).show();
			MapPoint point =  MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude());
			mapView.setMapCenterPoint(point, true);
			// 추적 버튼 풀기
			traceBtn.setClickable(true);
			// 이벤트 리스너 달아주기
	        traceBtn.setOnClickListener(WardModeActivity.this);

			mLocation = location;	// 위치 받아오기
		}

		public void onProviderDisabled(String provider) {
			Log.w(DEBUG_TAG, "onProviderDisabled");
		}

		public void onProviderEnabled(String provider) {
			Log.w(DEBUG_TAG, "onProviderEnabled");
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.w(DEBUG_TAG, "onStatusChanged");
		}
	};	

	public void onCalloutBalloonOfPOIItemTouched(MapView arg0, MapPOIItem arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onDraggablePOIItemMoved(MapView arg0, MapPOIItem arg1,
			MapPoint arg2) {
		// TODO Auto-generated method stub
		
	}

	public void onPOIItemSelected(MapView arg0, MapPOIItem arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onCurrentLocationDeviceHeadingUpdate(MapView arg0, float arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onCurrentLocationUpdate(MapView arg0, MapPoint arg1, float arg2) {
		// TODO Auto-generated method stub
		
	}

	public void onCurrentLocationUpdateCancelled(MapView arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onCurrentLocationUpdateFailed(MapView arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onMapViewCenterPointMoved(MapView arg0, MapPoint arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onMapViewDoubleTapped(MapView arg0, MapPoint arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onMapViewLongPressed(MapView arg0, MapPoint arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onMapViewSingleTapped(MapView arg0, MapPoint arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onMapViewZoomLevelChanged(MapView arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 키 인증 처리
	 */
	public void onDaumMapOpenAPIKeyAuthenticationResult(MapView v, int arg1,
			String arg2) {
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
					mapView.setMapCenterPoint(point, true);
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
		mapView.removeAllPolylines();
		for(MapPoint p: pathList){
			tracePath.addPoint(p);
		}
		// 맵뷰에 붙여준다.
		mapView.addPolyline(tracePath);		
		
	}	
	
	/**
	 * 위치정보의 보다 정확한 수신을 위해
	 * 이전 위치값과 비교한다.
	 * @param location
	 * @param currentBestLocation
	 * @return
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation){
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
	private boolean isSameProvider(String provider1, String provider2){
		if(provider1 == null){
			return provider2 == null;
		}
		return provider1.equals(provider2);
		
	}

	public void onClick(View v) {
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
				((Button)v).setText("도착");
			}else{
				endTrace();	
				isEnded = true;
				// 도착완료이면 클릭 버튼을 숨긴다.
				v.setVisibility(View.GONE);
				// 위치 리스너 제거
				 locationManager.removeUpdates(loclistener);
			}
			break;
		case R.id.loc_btn :	// 현재 위치 찾기
			if(mLocation != null){	// 현재 위치 정보가 있을때만
				MapPoint point =  MapPoint.mapPointWithGeoCoord(mLocation.getLatitude(), mLocation.getLongitude());
				mapView.setMapCenterPoint(point, true);
				Toast.makeText(this, "현재 위치로 이동합니다.", Toast.LENGTH_SHORT).show();
			}else{		
				// 현재 위치 수신이 아직 되지 않았을 경우
				Toast.makeText(this, "현재를 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
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
		mapView.addPOIItem(item);
        doStartService();	// 서비스로 위치수신		
		startTimeTv.setText("출발시간 : " + new SimpleDateFormat("hh시 mm분 ss초").format(new Date()));
		uts = new UploadToServer(point, "start", startPlaceTv.getText().toString(), startTimeTv.getText().toString());
		uts.start();
		
	}
	
	/**
	 * 추적 종료시 종료 오버레이 아이템을 생성하고 맵뷰에 븉여준다.
	 */
	private void endTrace() {
		// 현재 위치를 얻고
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
		mapView.addPOIItem(item);
		
		// 서비스설정
		Intent serviceIntent = new Intent(this, LocationService.class);
		// 서비스 종료
		stopService(serviceIntent);	
		endTimeTv.setText("도착시간 : " + new SimpleDateFormat("hh시 mm분 ss초").format(new Date()));
		uts = new UploadToServer(point, "end", endPlaceTv.getText().toString(), endTimeTv.getText().toString());
		uts.start();		
	}
	

	@Override
	public void onBackPressed() {	//  뒤로 가기버튼 클릭시 종료 여부
		// 도착 처리를 했으면 앱 종료 처리를 묻고
		// 그렇지 않으면 종료확인을 묻지 않고 나간다.
		/*
		if(isEnded == false){
			return;
		}
		*/
		finishDialog(this);
	}	
	
	
	
	
    /**
     * 옵션 메뉴 처리
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
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
    public boolean onOptionsItemSelected(MenuItem item){
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
    				public void onClick(DialogInterface dialog, int which) {
    					switch (which) {
    					case 0: // Standard
    						mapView.setMapType(MapView.MapType.Standard);
    						break;
    					case 1: // Satellite
    						mapView.setMapType(MapView.MapType.Satellite);
    						break;
    					case 2: // Hybrid
    						mapView.setMapType(MapView.MapType.Hybrid);
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

	
	public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder rGeoCoder, String addressString) {

		/*
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setTitle("DaumMapLibrarySample");
		alertDialog.setMessage(alertMessage);
		alertDialog.setPositiveButton("OK", null);
		alertDialog.show();
		*/
		Toast.makeText(this, addressString, Toast.LENGTH_SHORT).show();
		// 시작 poiitem 을 찾는다.
		MapPOIItem item = mapView.findPOIItemByTag(START_TAG);
		item.setItemName(addressString);
		mapView.addPOIItem(item);
		
		if(isEnded == false){
			startPlaceTv.setText("출발 위치 : " + addressString);
		}else{
			endPlaceTv.setText("도착 위치 : " + addressString);
		}
		
		reverseGeoCoder = null;
	}
	

	public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder rGeoCoder) {
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
		private MapPoint point;
		private String flag;
		private String place;
		private String time;
		public UploadToServer(MapPoint point, String flag, String place, String time){
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

	
}
