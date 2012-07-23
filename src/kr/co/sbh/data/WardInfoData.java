package kr.co.sbh.data;

/**
 * 피보호자 정보 데이터
 */
public class WardInfoData {
	private String name;				// 이름
	private String tel;					// 전화번호
	private String address;				// 주소
	private String email;				// 이메일
	private String desc;				// 특징
	private String imageFileName;		// 이미지파일명

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(final String tel) {
		this.tel = tel;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(final String address) {
		this.address = address;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(final String desc) {
		this.desc = desc;
	}

	public String getImageFileName() {
		return imageFileName;
	}

	public void setImageFileName(final String imageFileName) {
		this.imageFileName = imageFileName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
