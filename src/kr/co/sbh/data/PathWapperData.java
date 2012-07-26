package kr.co.sbh.data;

import java.io.Serializable;
import java.util.ArrayList;

import net.daum.mf.map.api.MapPoint;

public class PathWapperData implements Serializable {
	public ArrayList<MapPoint> pathList;

	public ArrayList<MapPoint> getPathList() {
		return pathList;
	}

	public void setPathList(ArrayList<MapPoint> pathList) {
		this.pathList = pathList;
	}
	
	
}
