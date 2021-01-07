package jpa.autocode.util;

import jpa.autocode.bean.Parms;

import java.util.ArrayList;
import java.util.List;

public class ParmsUtil {

    public static List<String> getValueByKey(List<Parms> parms, String names) {
        List<String> values = new ArrayList<>();
        parms.forEach(p -> {
            if (p.getName().equals(names)) {
                values.add(p.getValue());
            }
        });
        return values;
    }
}
