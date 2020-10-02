package jpa.autocode.util;

public class StringUtil {

    public static String objToStr(Object obj) {
        return obj == null ? null: obj.toString();
    }

    public static String firstLetterUppercase(String str) {
        StringBuffer sb = new StringBuffer();
        sb.append(str.substring(0, 1).toUpperCase() + str.substring(1));
        return sb.toString();
    }

    public static String firstLetterLowerCase(String str) {
        StringBuffer sb = new StringBuffer();
        sb.append(str.substring(0, 1).toLowerCase() + str.substring(1));
        return sb.toString();
    }

    public static boolean isEmpty(String str) {
    	return str == null || str.length() == 0;
    }
}
