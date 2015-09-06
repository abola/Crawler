package co.gibar.travel;

import co.gibar.crawler.Crawler;
import co.gibar.crawler.FBCrawler;
import co.gibar.crawler.JsonTools;
import co.gibar.crawler.StoreTools;
import co.gibar.datasource.MySQLDataSource;
import com.google.common.collect.Lists;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by abola on 2015/9/4.
 */
public class PostPeopleTracker {

    private Crawler crawl ;

    private String clientId;
    private String clientSecret;

    private String longToken;

    SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat normalDateTime = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
    SimpleDateFormat normalDate = new SimpleDateFormat("yyyy-MM-dd");

    public static final long HOUR = 3600*1000; // in milli-seconds.


    public PostPeopleTracker(){
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

    private Map<String, Object> loadPostComments(String postId){
        List<Map<String, Object>> jsonResult = crawl.crawlJson(postId + "?fields=comments.limit(1000){from{id},comments.limit(1000){from{id},likes.limit(1000)},likes.limit(1000){id}}");

        return jsonResult.get(0);
    }


    private Map<String, Object> loadPostCommentsAfter(String postId, String after){
        List<Map<String, Object>> jsonResult = crawl.crawlJson(postId + "?fields=comments.limit(1000){from{id},comments.limit(1000){from{id},likes.limit(1000)},likes.limit(1000){id}}&after="+after);

        // 下一頁的處理，可能會有 code 2 的錯誤
        return jsonResult.get(0);
    }

    private List<Map<String, Object>> load7DaysPost(){
        String sqlLoadNeedUpdate = ""
                +" SELECT series.* "
                +" FROM `page_posts` posts "
                +"    , `posts_series` series "
                +" WHERE "
                +"   posts.id = series.id "
                +"   and posts.post_id = series.post_id "
                +"   and posts.deprecated = 0 "
                +"   and series.series_type = '6' " +
                "    and posts.settled = 0 "
                +" LIMIT 1";

        try {
            List<Map<String, Object>> results = MySQLDataSource.executeQuery(sqlLoadNeedUpdate, MySQLDataSource.connectToGibarCoDB);

            return results;
        }catch(Exception ex){
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return Lists.newArrayList();
        }
    }

    public PostPeopleTracker sync(){

//        List<Map<String, Object>> posts = loadNeedUpdatePost();
//
//        for( Map<String, Object> post : posts ) {
//            String postId = post.get("post_id").toString();
//            String seriesType = post.get("series").toString();
//
//            System.out.println("process postId: " + postId);
//
//            updateAll( Lists.newArrayList(callGraphAPI(postId)), seriesType );
//        }
//fields=likes{id},comments.limit(2){from{id},likes.limit(0).summary(true)}
//        StoreTools.startStdoutTo("/tmp/testme");
//
//        System.out.println(this.clientId);
//        System.out.append(this.clientId);
//        System.out.append(this.clientSecret);
//
//        System.out.flush();
//
//        StoreTools.resetStdout();
//
//        System.out.append(this.clientId);
//        System.out.append(this.clientSecret);
//
//
//        System.out.flush();


        List<Map<String, Object>> posts = load7DaysPost();

        for(Map<String, Object> post : posts){
            String id = JsonTools.getJsonPathValue(post, "id", "");
            String postId = JsonTools.getJsonPathValue(post, "post_id", "");

            // load comments
            Map<String, Object> comments = this.loadPostComments( postId );

            procComments(comments, id, postId);

        }


//        crawl.crawlJson("131181017972_10153548851512973/like");

        return this;
    }

    private void procComments(Map<String, Object> comments, String id, String postId) {
        procComments(comments, id, postId, "comment.from");
    }

    private void procComments(Map<String, Object> comments, String id, String postId, String lv2){
        List<Map<String, Object>> dataList = JsonTools.getJsonPathListMap( comments, "comments.data" );

        try {
            for (Map<String, Object> data : dataList) {
                String commentFromId = JsonTools.getJsonPathValue(data, "from.id", "");

                // comment from
                append(id, postId, "comments", lv2, commentFromId);


                try {
                    List<Map<String, Object>> commentLikes = JsonTools.getJsonPathListMap(data, "likes.data");
                    for (Map<String, Object> commentLike : commentLikes) {
                        String likeFromId = JsonTools.getJsonPathValue(commentLike, "id", "");
                        append(id, postId, "likes", "comment.like", likeFromId);
                    }
                } catch (Exception ex) {
                }


                // try parse comments comment
                if (!"sub_comment.from".equals(lv2))
                    procComments(data, id, postId, "sub_comment.from");
            }
        }catch(Exception ex){

        }
        stdoutFlush();
    }

    private void append(String id, String postId, String lv1, String lv2, String from){
//        System.out.append("\n");
        System.out.println(id + "," +postId+","+lv1+","+lv2+","+from);
    }

    private void stdoutStart(String path){
        StoreTools.startStdoutTo(path);
    }

    private void stdoutFlush(){
        System.out.flush();
    }

    private void stdoutStop(){
        StoreTools.resetStdout();
    }

    public static PostPeopleTracker create(){
        return new PostPeopleTracker();
    }

    public static void main(String[] argv){
        PostPeopleTracker
                .create()
                .sync()
        ;
    }
}
