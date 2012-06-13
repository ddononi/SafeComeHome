package kr.co.sbh;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyUtils {


	/**
	 * 이메일 체크
	 */
	public static boolean isEmailValid(final String email) {
	    boolean isValid = false;

	    String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
	    CharSequence inputStr = email;

	    Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(inputStr);
	    if (matcher.matches()) {
	        isValid = true;
	    }
	    return isValid;
	}
}
