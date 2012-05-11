package kr.co.sbh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import kr.co.sbh.data.PathPoint;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPOIItem.MarkerType;
import net.daum.mf.map.api.MapPOIItem.ShowAnimationType;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;
import net.daum.mf.map.api.MapView.CurrentLocationEventListener;
import net.daum.mf.map.api.MapView.MapViewEventListener;
import net.daum.mf.map.api.MapView.OpenAPIKeyAuthenticationResultListener;
import net.daum.mf.map.api.MapView.POIItemEventListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * 보호자 모드 엑티비티
 * 디비에서 피보호자의 이동경로를 받아와서
 * 출발 및 도착 경로를 그려준다.
 */
public class PathActivity extends BaseActivity implements
OpenAPIKeyAuthenticationResultListener, MapViewEventListener,
CurrentLocationEventListener, POIItemEventListener {
	private MapView mMapView = null;
	private final ProgressDialog dialog = null;
	private MapPoint.GeoCoordinate mapPointGeo;	// 현위치를 받을 point 객체
	// ui 처리를 위한 핸들러
	private final Handler handler = new Handler() {
    	@Override
		public void handleMessage(final Message msg) {
    		@SuppressWarnings("unchecked")
    		// 핸들러를 통해 path를 그려준다.
			List<PathPoint> list = (List<PathPoint>)msg.obj;
    		drawPath(list);
    	}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.path_layout);
		initLayout();
	}



	/**
	 * 맵 키 설정 , 이벤트 설정 , 기타 초기화 설정
	 */
	private void initLayout() {
		// TODO Auto-generated method stub
		LinearLayout mapParant = (LinearLayout)findViewById(R.id.map_root);
		// map 설정
		mMapView = new MapView(this);
		mMapView.setDaumMapApiKey(MAP_KEY);
		mMapView.setOpenAPIKeyAuthenticationResultListener(this);
		mMapView.setMapViewEventListener(this);
		mMapView.setCurrentLocationEventListener(this);
		mMapView.setPOIItemEventListener(this);
		mMapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.570705,126.958008), true);	// 초기 지도 위치 설정
		mMapView.setCurrentLocationEventListener(this);
		mMapView.setPOIItemEventListener(this);
		mapParant.addView(mMapView);
		// 쓰레드로 서버에서 경로를 받아온다.
		new LoadPathThread().start();

	}



	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}


	/**
	 * 키 인증 처리 결과
	 * 인증 성공시 현위치를 검색하고 주변 친구들을 인증실패시 종료한다.
	 */
	public void onDaumMapOpenAPIKeyAuthenticationResult(final MapView arg0, final int result,
			final String arg2) {
		// 인증 성공시에 현위치 검색
		if(result == API_RESULT_OK){	// 인증 성공
			mMapView.setZoomLevel(4, true);	// 줌레벨 4단계로
		}else{
			finish();	// 앱종료
		}
	}


	/**
	 * 이동경로 그려주기
	 */
	private void drawPath(final List<PathPoint> list){
		MapPolyline polyline = new MapPolyline();
		mMapView.removeAllPolylines();	// 이전 지적선이 있으면 지워준다.
		polyline.setTag(1);
		polyline.setLineColor(Color.argb(128, 255, 0, 0));
		// list에서 좌표를 가져와 polyline에 넣어준다.
		int index = 0;
		for(PathPoint p :list){
			if(index == 0){	// 출발 아이콘
				// 출발 아이콘 처리
        		MapPOIItem item = new MapPOIItem();
        		// poi 아이템 설정
        		item.setTag(START_TAG);
        		item.setItemName("출발");
        		Toast.makeText(this, "gg", Toast.LENGTH_SHORT).show();
        		item.setMapPoint(MapPoint.mapPointWithGeoCoord(p.getLatitude(), p.getLongitude()));
        		item.setShowAnimationType(ShowAnimationType.SpringFromGround);
        		item.setMarkerType(MarkerType.CustomImage);
        		item.setCustomImageResourceId(R.drawable.custom_poi_marker_start);
        		item.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(22,0));
        		// 맵에 붙여준다.
        		mMapView.addPOIItem(item);
			}else if(index == list.size() -1){
				//if(p.getPathFlag() != null &&
				//	p.getPathFlag().equals("arrive")){	// 도착 아이콘
				// 도착 아이콘 처리
				MapPOIItem endItem = new MapPOIItem();
				// poi 아이템 설정
				endItem.setTag(END_TAG);
				endItem.setItemName("도착");
				endItem.setMapPoint(MapPoint.mapPointWithGeoCoord(p.getLatitude(), p.getLongitude()));
				endItem.setShowAnimationType(ShowAnimationType.SpringFromGround);
				endItem.setMarkerType(MarkerType.CustomImage);
				endItem.setCustomImageResourceId(R.drawable.custom_poi_marker_end);
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

	/**
	 * 두 Location 간에 거리 구하기
	 * @param sLat
	 * @param sLong
	 * @param dLat
	 * @param dLong
	 * @return
	 */
	private static double getDistanceArc(final double sLat, final double sLong, final double dLat, final double dLong){
        final int radius=6371009;

        double uLat=Math.toRadians(sLat-dLat);
        double uLong=Math.toRadians(sLong-dLong);
        double a = Math.sin(uLat/2) * Math.sin(uLat/2) + Math.cos(Math.toRadians(sLong)) * Math.cos(Math.toRadians(dLong)) * Math.sin(uLong/2) * Math.sin(uLong/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = radius * c;
        return Double.parseDouble(String.format("%.3f", distance/1000));
    }



	/**
	 *  get방식으로 서버에서 조회값을 보내  서버에서 생성된 xml를 parsing하여
	 *  접속하고 있는 전체 친구리스트를 가져온다.
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
		    parser.setInput(new StringReader(decodeXMl));
			int eventType = -1;
			// 리스트에 담을 좌표 데이터 클래스
			PathPoint point = null;

			// xml 노드를 순회하면서 위경도좌표와 시각을 가져와 리스트에 담는다.
			while(eventType != XmlResourceParser.END_DOCUMENT){	// 문서의 마지막이 아닐때까지
				if(eventType == XmlResourceParser.START_TAG){	// 이벤트가 시작태그면
					String strName = parser.getName();
					if(strName.contains("lat")){				// 위도
						point = new PathPoint();
						point.setLatitude(Double.valueOf(parser.nextText()));
					}else if(strName.equals("lng")){			// 경도
						point.setLongitude(Double.valueOf(parser.nextText()));
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


	@Override
	public void onBackPressed() {	//  뒤로 가기버튼 클릭시 종료 여부
		finishDialog(this);
	}



    /**
     * 옵션 메뉴 생성
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu){
    	super.onCreateOptionsMenu(menu);
    	//menu.add(0, VIEW_MAP, 0, "이동경로").setIcon(android.R.drawable.ic_menu_info_details);
    	menu.add(0, STANDARD, 0, "일반지도");
    	menu.add(0, SATELLITE, 0, "위성지도");
    	menu.add(0, HYBRID, 0, "하이브리드");
    	menu.add(0, INFO_VIEW,0, "피보호자정보");//.setIcon(android.R.drawable.ic_menu_help);
    	//item.setIcon();
    	return true;
    }


    /**
     * 옵션 메뉴 선택시 처리
     * 1번 선택시 수강선택,
     * 2번 선택시 주변 친구 범위 선택
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item){
		Intent intent;
    	switch(item.getItemId()){
    		case INFO_VIEW:	// 피보호자 정보 엑팁티로
    			intent = new Intent(this, InfoActivity.class);
    			startActivity(intent);
    			break;
    		case STANDARD :
    			mMapView.setMapType(MapView.MapType.Standard);
    			break;
    		case SATELLITE :
    			mMapView.setMapType(MapView.MapType.Satellite);
    			break;
    		case HYBRID :
    			mMapView.setMapType(MapView.MapType.Hybrid);
    			break;
    	}
    	return true;
    }


    /**
     *	이동 경로를 가져올 쓰레드
     */
    private class LoadPathThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			List<PathPoint> list;
			try {
				list = processXML();
				Message msg = new Message();
				msg.obj = list;
				handler.sendMessage(msg);
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			super.run();
		}

    }

	public void onCalloutBalloonOfPOIItemTouched(final MapView arg0, final MapPOIItem arg1) {
	}

	public void onDraggablePOIItemMoved(final MapView arg0, final MapPOIItem arg1,
			final MapPoint arg2) {
	}

	public void onPOIItemSelected(final MapView arg0, final MapPOIItem arg1) {
	}

	public void onCurrentLocationDeviceHeadingUpdate(final MapView arg0, final float arg1) {
	}

	public void onCurrentLocationUpdate(final MapView arg0, final MapPoint arg1, final float arg2) {
	}

	public void onCurrentLocationUpdateCancelled(final MapView arg0) {
	}

	public void onCurrentLocationUpdateFailed(final MapView arg0) {
	}

	public void onMapViewCenterPointMoved(final MapView arg0, final MapPoint arg1) {

	}

	public void onMapViewDoubleTapped(final MapView arg0, final MapPoint arg1) {
	}

	public void onMapViewLongPressed(final MapView arg0, final MapPoint arg1) {
	}

	public void onMapViewSingleTapped(final MapView arg0, final MapPoint arg1) {
	}

	public void onMapViewZoomLevelChanged(final MapView arg0, final int arg1) {
	}

}

