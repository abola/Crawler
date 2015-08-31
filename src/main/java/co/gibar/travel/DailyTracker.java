package co.gibar.travel;

import co.gibar.crawler.Crawler;
import co.gibar.crawler.FBCrawler;
import co.gibar.datasource.MySQLDataSource;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Map;

/**
 * Created by abola on 2015/8/30.
 */
public class DailyTracker {


    private Crawler crawl ;

    public DailyTracker(){
        String clientId = "626465174161774";
        String clientSecret = "dbd550847406c13cd1da4085331ab54e";
        crawl = new FBCrawler(clientId, clientSecret);

    }

    public DailyTracker sync(){

        List<String> register = getRegisterList();

        List<Map<String, Object>> resultList = Lists.newArrayList();
        for( String id : register ) {
            resultList.add(callGraphAPI(id));
        }

        updateAll(resultList);

        return this;
    }

    public ImmutableMap<String, Object> callGraphAPI(String id){

        List<ImmutableMap<String,Object>> jsonResult = crawl.crawlJson(id + "?fields=id,name,location,general_info,likes,link,checkins,cover,category,category_list,website,description,talking_about_count");

        return jsonResult.get(0);
    }

    public void updateAll(List<Map<String, Object>> resultList){
        List<String> executeSql = Lists.newArrayList();
        for( Map<String, Object> result : resultList){

            String id  = result.get("id").toString();
            String name = result.get("name").toString();
            String link = result.get("link").toString();
            String category = result.get("category").toString();

            // option
            String lat = ((Map)result.get("location")).get("latitude").toString();
            String lng = ((Map)result.get("location")).get("longitude").toString();
            String likes = result.get("likes").toString();
            String cover = result.get("cover").toString();
            String checkins = result.get("checkins").toString();
            String talking_about_count = result.get("talking_about_count").toString();
            String website = result.get("website").toString();

            String insertOrUpdatePoint =
                    "insert into point(id,name,lat,lng,link,cover,category,description,website) " +
                    "values("+id+",'"+name+"',"+lat+","+lng+",'"+link+"','"+cover+"','"+category+"','','"+website+"')";

            System.out.println(insertOrUpdatePoint);
        }


    }

    public List<String> getRegisterList(){
        String sqlLoadRegisterList = "select `alias` from `register_table` where `suspend` = 0 ";

        try {
            List<Map<String, Object>> results = MySQLDataSource.executeQuery(sqlLoadRegisterList, MySQLDataSource.connectToGibarCoDB);

            List<String> registerList = Lists.newArrayList();
            for(Map<String, Object> register: results){
                String alias = register.get("alias").toString();
                registerList.add(alias);
            }
            return registerList;
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }

    }

    public static DailyTracker create(){
        return new DailyTracker();
    }

    public static void main(String[] argv){
        DailyTracker
                .create()
                .sync()
        ;
    }
}
