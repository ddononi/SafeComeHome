/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.co.sbh;

import android.location.Location;


/**
 * Library for some use useful latitude/longitude math
 */
public class GeoUtils {
    private static int EARTH_RADIUS_KM = 6371;

    public static int MILLION = 1000000;

    /**
     * Computes the distance in kilometers between two points on Earth.
     * 
     * @param lat1 Latitude of the first point
     * @param lon1 Longitude of the first point
     * @param lat2 Latitude of the second point
     * @param lon2 Longitude of the second point
     * @return Distance between the two points in kilometers.
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        return Math.acos(Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.cos(deltaLonRad))
                * EARTH_RADIUS_KM;
    }
 
    
    /**
     * Computes the bearing in degrees between two points on Earth.
     * 
     * @param lat1 Latitude of the first point
     * @param lon1 Longitude of the first point
     * @param lat2 Latitude of the second point
     * @param lon2 Longitude of the second point
     * @return Bearing between the two points in degrees. A value of 0 means due
     *         north.
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLonRad) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(lat2Rad)
                * Math.cos(deltaLonRad);
        return radToBearing(Math.atan2(y, x));
    }
    
    /**
     * Converts an angle in radians to degrees
     */
    public static double radToBearing(double rad) {
        return (Math.toDegrees(rad) + 360) % 360;
    }
    
	/**
	 * 위치정보의 보다 정확한 수신을 위해
	 * 이전 위치값과 비교한다.
	 * @param location
	 * @param currentBestLocation
	 * @return
	 */
    public static boolean isBetterLocation(Location location, Location currentBestLocation){
		if(currentBestLocation == null){
			// 잘못된 위치라도 없는것보다 좋다.
			return true;
		}
		
		// 기존 정보와 새로운 정보간의 시간 차이를 츨정한다.
		// 2분 이상 경과시 새로운 장소로 이동하였을 가능성이 크므로
		// 해당 정보를 채택하게 된다.
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > BaseActivity.TWO_MINUTES;	// 새로운 위치정보여부
		boolean isSignificantlyOlder = timeDelta < -BaseActivity.TWO_MINUTES;
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
		boolean isSignificantlyLessAccurate = accuracyDelta > BaseActivity.ACCURATE_VALUE;
		
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
	private static boolean isSameProvider(String provider1, String provider2){
		if(provider1 == null){
			return provider2 == null;
		}
		return provider1.equals(provider2);
		
	}	

	/**
	 * 두지점간의 거리 구하기
	 * 
	 * @param sLat
	 * @param sLong
	 * @param dLat
	 * @param dLong
	 * @return
	 */
	public static double getDistance_arc(final double sLat,
			final double sLong, final double dLat, final double dLong) {
		final int radius = 6371009;

		double uLat = Math.toRadians(sLat - dLat);
		double uLong = Math.toRadians(sLong - dLong);
		double a = Math.sin(uLat / 2) * Math.sin(uLat / 2)
				+ Math.cos(Math.toRadians(sLong))
				* Math.cos(Math.toRadians(dLong)) * Math.sin(uLong / 2)
				* Math.sin(uLong / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = radius * c;

		return Double.parseDouble(String.format("%.3f", distance / 1000));
	}    
}