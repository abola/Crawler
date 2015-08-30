package co.gibar.travel;

import co.gibar.crawler.Crawler;
import co.gibar.crawler.FBCrawler;
import co.gibar.datasource.MySQLDataSource;
import com.google.common.collect.Lists;

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

        for( String id : register ) {
            callGraphAPI(id);
        }

        return this;
    }

    public Map<String, String> callGraphAPI(String id){


        String jsonResult = crawl.crawl(id + "?fields=id,name,location,general_info,likes,link,checkins,cover,category,category_list,website,description,talking_about_count");

        System.out.println(jsonResult);


        return null;
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
