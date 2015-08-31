package co.gibar.crawler;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Created by abolalee on 2015/8/31.
 */
public class JsonTools {


    public static String getJsonPathValue(Map<String, Object> jsonObject, String path, String defaultOnNull){
        try {
            if (path.indexOf(".") < 0) return jsonObject.get(path).toString();

            List<String> objs = Lists.newArrayList(path.split("\\."));

            Object current = null;
            for ( String obj:objs ){
                if (null == current){
                    current = jsonObject.get( obj );
                }else{
                    current = ((Map<String, Object>)current).get(obj);
                }
            }

            return current.toString();
        }catch(Exception ex){
            return defaultOnNull;
        }
    }

}
