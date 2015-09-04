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

            System.out.println("process postId: " + postId + ", series: " + seriesType);

            try {
                updateAll(Lists.newArrayList(callGraphAPI(postId)), postId, seriesType);
            }catch(Exception ex) {
                deprecatedThis(postId);
                continue;
            }

        }


        return this;
    }

    private void deprecatedThis(String postId){
        String sqlUpdatePagePostsToDeprecated =
                "update `page_posts` set `deprecated` = 1 where post_id = '"+postId+"';";
        System.out.println("deprecate post: " + postId);

        try {
            MySQLDataSource.execute(sqlUpdatePagePostsToDeprecated, MySQLDataSource.connectToGibarCoDB);
        }catch(Exception ex){}

    }

    public Map<String, Object> callGraphAPI(String postId) {
        List<Map<String, Object>> jsonResult = crawl.crawlJson(postId + "?fields=id,name,shares,likes.limit(0).summary(1),comments.limit(0).summary(1)");
        return jsonResult.get(0);
    }


    private List<Map<String, Object>> loadNeedUpdatePost(){
        String sqlLoadNeedUpdate = ""
                +" SELECT FLOOR(HOUR(TIMEDIFF( NOW(),`created_time` ))/24) AS series, A. * "
                +" FROM  `page_posts` A "
                +" WHERE NOT DATEDIFF(  `last_update` ,  `created_time` ) >=7 "
                +" AND HOUR(TIMEDIFF( NOW() ,  `created_time` ))  > 24 "
                +" AND HOUR(TIMEDIFF( NOW() ,  `created_time` )) - HOUR(TIMEDIFF(`last_update`, `created_time` )) > 24 "
                +" AND NOT `deprecated` = 1"
                +" LIMIT 30 ";

        try {
            List<Map<String, Object>> results = MySQLDataSource.executeQuery(sqlLoadNeedUpdate, MySQLDataSource.connectToGibarCoDB);

            return results;
        }catch(Exception ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return Lists.newArrayList();
        }
    }

    public void updateAll(List<Map<String, Object>> resultList, String postId, String seriesType) {
        List<String> executeSql = Lists.newArrayList();
        for (Map<String, Object> result : resultList) {

//            String postId  = result.get("id").toString();
            String id = postId.split("_")[0];

            String shares = JsonTools.getJsonPathValue(result, "shares.count", "0");
            String likes = JsonTools.getJsonPathValue(result, "likes.summary.total_count","0");
            String comments = JsonTools.getJsonPathValue(result, "comments.summary.total_count","0");

            String insertOrUpdatePageVolume =
                    "insert into `posts_series`(id,post_id,series_type,shares,likes,comments) " +
                    "values("+id+",'"+postId+"',"+seriesType+","+shares+","+likes+","+comments+") " +
                    "on duplicate key update shares=values(shares), likes=values(likes), comments=values(comments) ;" ;

            String updatePagePost = "update `page_posts` set last_update= now() where post_id = '"+postId+"' ;";




            executeSql.add( insertOrUpdatePageVolume );
            executeSql.add( updatePagePost );
        }

//        System.out.print(executeSql.toString());

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
