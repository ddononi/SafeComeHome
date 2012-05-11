package kr.co.sbh.data;

/**
 *	�̵���θ� ���� ������ Ŭ����
 */
public class PathPoint {
	private double latitude;	// ����
	private double longitude;	// �浵
	private String Date;		// �ð�
	private String pathFlag;	// �÷���

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getDate() {
		return Date;
	}

	public void setLatitude(final double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(final double longitude) {
		this.longitude = longitude;
	}

	public void setDate(final String date) {
		Date = date;
	}

	public String getPathFlag() {
		return pathFlag;
	}

	public void setPathFlag(final String pathFlag) {
		this.pathFlag = pathFlag;
	}



}
