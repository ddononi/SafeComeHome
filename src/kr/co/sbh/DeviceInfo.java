package kr.co.sbh;

import android.telephony.TelephonyManager;

/*
 *  ����̽� ���� ��������
 *  �޴��� ��ȣ, �ܸ����ȣ, SIM ����
 */
public class DeviceInfo{
	private static TelephonyManager _telephony = null;
	private static DeviceInfo di = new DeviceInfo();
	private DeviceInfo(){
		
	}
	
	public static DeviceInfo setDeviceInfo(TelephonyManager telphony){
		_telephony = telphony;
		return di;
	}
	
	public String getDeviceNumber(){
		String phoneNum = (_telephony != null)?_telephony.getLine1Number():"";
		//String cellNum = phoneNum.substring(phoneNum.length()-8, phoneNum.length());
		String cellNum = phoneNum.replace("+82", "0");
		return cellNum;	// �ڵ��� ��ȣ�� �°� �Ѱ��� ex) 010,011
	}
	/*
	public String getDeviceNumber2(){
		String phoneNum = (_telephony != null)?_telephony.getLine1Number():"";
		//String cellNum = phoneNum.substring(phoneNum.length()-8, phoneNum.length());
		
		return phoneNum;	// �ڵ��� ��ȣ�� �°� �Ѱ��� ex) 010,011
	}	
*/
	public String getMyDeviceID(){	
		return (_telephony != null)? _telephony.getDeviceId():"";
	}
	
	public String myDeviceSIM(){
		return (_telephony != null)? _telephony.getSimSerialNumber():"";
	}

}
