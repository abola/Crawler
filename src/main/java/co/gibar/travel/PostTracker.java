package co.gibar.travel;

import co.gibar.crawler.Crawler;
import co.gibar.crawler.FBCrawler;
import co.gibar.crawler.JsonTools;
import co.gibar.datasource.MySQLDataSource;
import com.google.common.collect.Lists;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by abolalee on 2015/9/3.
 */
public class PostTracker {

    static String SERIES_TYPE_FIRST_DAY = "1";
    static String SERIES_TYPE_SECOND_DAY = "2";
    static String SERIES_TYPE_TRIPLE_DAY = "3";
    static String SERIES_TYPE_A_WEEK = "7";

    private Crawler crawl ;

    private String clientId;
    private String clientSecret;

    private String longToken;

    SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat normalDateTime = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
    SimpleDateFormat normalDate = new SimpleDateFormat("yyyy-MM-dd");

    public static final long HOUR = 3600*1000; // in milli-seconds.


    public PostTracker(){
        loadConfiguration();

        if ( null !=  longToken && !"".equals(longToken))
            crawl = new FBCrawler(longToken);
        else
            crawl = new FBCrawler(clientId, clientSecret);
    }

    public void loadConfiguration(){
        String sqlLoadConfiguration = "select * from `configuration` where `key` in ('client_id','client_secret','long_token')";
        try {

            System.out.println("Start load configurations. ");
            List<Map<String, Object>> results = MySQLDataSource.executeQuery(sqlLoadConfiguration, MySQLDataSource.connectToGibarCoDB);

            System.out.println("Configurations loaded ");
            for( Map<String, Object> setting: results ){
                if ( "client_id".equals(setting.get("key").toString()) ) this.clientId = setting.get("value").toString();
                if ( "client_secret".equals(setting.get("key").toString()) ) this.clientSecret = setting.get("value").toString();
                if ( "long_token".equals(setting.get("key").toString()) ) this.longToken = setting.get("value").toString();
            }
        }catch(Exception ex){
            // throw ConfigurationException
        }
    }

    public PostTracker sync(){

        List<Map<String, Object>> posts = loadNeedUpdatePost();

        for( Map<String, Object> post : posts ) {
            String postId = post.get("post_id").toString();
            String seriesType = post.get("series").toString();

            System.out.println("process postId: " + postId);

            updateAll( Lists.newArrayList(callGraphAPI(postId) ), seriesType );
        }


        return this;
    }


    public Map<String, Object> callGraphAPI(String postId){
        List<Map<String, Object>> jsonResult = crawl.crawlJson(postId + "?fields=id,name,shares,likes.limit(0).summary(1),comments.limit(0).summary(1)");
        return jsonResult.get(0);
    }


    private List<Map<String, Object>> loadNeedUpdatePost(){
        String sqlLoadNeedUpdate = ""
                +" SELECT DATEDIFF( NOW( ) ,  `created_time` ) - DATEDIFF(  `last_update` ,  `created_time` ) AS series, A. * "
                +" FROM  `page_posts` A "
                +" WHERE NOT DATEDIFF(  `last_update` ,  `created_time` ) >=7 "
                +" AND DATEDIFF( NOW( ) ,  `created_time` ) -1 > 0 "
                +" AND DATEDIFF( NOW( ) ,  `created_time` ) - DATEDIFF(  `last_update` ,  `created_time` ) > 0 "
                +" LIMIT 30 ";

        try {
            List<Map<String, Object>> results = MySQLDataSource.executeQuery(sqlLoadNeedUpdate, MySQLDataSource.connectToGibarCoDB);

            return results;
        }catch(Exception ex){
            return Lists.newArrayList();
        }
    }

    public void updateAll(List<Map<String, Object>> resultList, String seriesType) {
        List<String> executeSql = Lists.newArrayList();
        for (Map<String, Object> result : resultList) {

            String postId  = result.get("id").toString();
            String id = postId.split("_")[0];

            String shares = JsonTools.getJsonPathValue(result, "shares.count", "0");
            String likes = JsonTools.getJsonPathValue(result, "likes.summary.total_count","0");
            String comments = JsonTools.getJsonPathValue(result, "likes.summary.total_count","0");

            String insertOrUpdatePageVolume =
                    "insert into `posts_series`(id,post_id,series_type,shares,likes,comments) " +
                    "values("+id+",'"+postId+"',"+seriesType+","+shares+","+likes+","+comments+") " +
                    "on duplicate key update shares=values(shares), likes=values(likes), comments=values(comments) ;" ;
            executeSql.add( insertOrUpdatePageVolume );
        }

        try {
            MySQLDataSource.execute( executeSql, MySQLDataSource.connectToGibarCoDB );
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public static PostTracker create(){
        return new PostTracker();
    }

    public static void main(String[] argv){
        PostTracker
                .create()
                .sync()
        ;
    }
}