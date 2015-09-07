package co.gibar.travel;

import co.gibar.crawler.FBCrawler;
import co.gibar.crawler.JsonTools;
import co.gibar.crawler.StoreTools;
import co.gibar.datasource.MySQLDataSource;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by abola on 2015/9/4.
 */
public class PostPeopleTracker {

    private FBCrawler crawl ;

    private String clientId;
    private String clientSecret;

    private String longToken;

    SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    SimpleDateFormat normalDateTime = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
    SimpleDateFormat normalDate = new SimpleDateFormat("yyyy-MM-dd");

    public static final long HOUR = 3600*1000; // in milli-seconds.

    String LV2_COMMMON_FROM = "comment.from";
    String LV2_SUBCOMMMON_FROM = "sub_comment.from";

    String LV2_LIKES_COMMON = "comment.like";
    String LV2_LIKES_POST = "post.list";


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


    private Map<String, Object> loadPostCommentsAfter(String next){
        List<Map<String, Object>> jsonResult = crawl.crawlJson( next );

        // 下一頁的處理，可能會有 code 2 的錯誤
        return jsonResult.get(0);
    }

    private Map<String, Object> loadPostLikes(String postId){
        List<Map<String, Object>> jsonResult = crawl.crawlJson(postId + "?fields=likes.limit(1000){id}");

        return jsonResult.get(0);
    }

    private Map<String, Object> loadPostLikesAfter(String next){
        try {
            List<Map<String, Object>> jsonResult = crawl.crawlJson(next);
            return jsonResult.get(0);
        }catch(Exception ex){
            if ( "2".equals( crawl.getGraphApiErrorCode() ) ){
                try {
                    Thread.currentThread().sleep(2000);
                    return loadPostLikesAfter(next);
                }catch(Exception sleepEx){
                    sleepEx.printStackTrace();
                    return null;
                }
            }
            ex.printStackTrace();
            return null;
        }
    }
    private List<Map<String, Object>> load7DaysPost(){
        String sqlLoadNeedUpdate = ""
                +" SELECT series.* "
                +" FROM `page_posts` posts "
                +"    , `posts_series` series "
                +"    , `political` political"
                +" WHERE "
                +"   posts.id = series.id "
                +"   and posts.post_id = series.post_id "
                +"   and posts.deprecated = 0 "
                +"   and series.series_type = '3' "
                +"   and posts.settled = 0 "
                +"   and posts.id = political.id"
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

    Multiset commentsCounter ;
    Multiset likesCounter ;
    Multiset replayCounter ;

    public PostPeopleTracker sync(){

        List<Map<String, Object>> posts = load7DaysPost();

        for(Map<String, Object> post : posts){

            commentsCounter =  HashMultiset.create();
            likesCounter =  HashMultiset.create();
            replayCounter = HashMultiset.create();

            String id = JsonTools.getJsonPathValue(post, "id", "");
            String postId = JsonTools.getJsonPathValue(post, "post_id", "");

            stdoutStart("/2016/" + postId);


            // load comments
            Map<String, Object> comments = this.loadPostComments(postId);

            procComments(comments, id, postId);


            Map<String, Object> likes = this.loadPostLikes(postId);
            procLikes(likes, id, postId);

            stdoutStop();


            Integer likes_all_total = likesCounter.size();
            Integer likes_fans_uniq = likesCounter.elementSet().size();
            Integer comments_all_total = commentsCounter.size();
            Integer comments_fans_uniq = commentsCounter.elementSet().size();

            Integer comments_replay_cnt = replayCounter.elementSet().size();

            String likesCount = JsonTools.getJsonPathValue(post, "likes", "0");
            String commentsCount = JsonTools.getJsonPathValue(post, "comments", "0");

            String insertOrUpdatePostSettle = "" +
                    "insert into posts_settle(id,post_id,likes_fans_uniq,comments_fans_uniq" +
                    ",comments_replay_cnt,likes_all_total,comments_all_total" +
                    ",likes,comments,settle_time) " +
                    "values("+id+",'"+postId+"',"+likes_fans_uniq+","+comments_fans_uniq+"" +
                    ","+comments_replay_cnt+","+likes_all_total+","+comments_all_total+"" +
                    ","+likesCount+","+commentsCount+",now()) " +
                    "on duplicate key update likes_fans_uniq=values(likes_fans_uniq)" +
                    ",comments_fans_uniq=values(comments_fans_uniq)" +
                    ",comments_replay_cnt=values(comments_replay_cnt)" +
                    ",likes_all_total=values(likes_all_total)" +
                    ",comments_all_total=values(comments_all_total)" +
                    ",likes=values(likes)" +
                    ",comments=values(comments)" +
                    ",settle_time=values(settle_time)" +
                    ";";

            String updatePagePosts = "update page_posts set settled=1 where id="+id+" and post_id = '"+postId+"';";

            try {
                MySQLDataSource.execute(Lists.newArrayList(insertOrUpdatePostSettle, updatePagePosts), MySQLDataSource.connectToGibarCoDB);
            }catch (Exception ex){
                ex.printStackTrace();

            }
        }


//        crawl.crawlJson("131181017972_10153548851512973/like");

        return this;
    }

    private void procComments(Map<String, Object> comments, String id, String postId) {
        procComments(comments, id, postId, LV2_COMMMON_FROM);
    }

    private void procComments(Map<String, Object> comments, String id, String postId, String lv2){
        List<Map<String, Object>> dataList = JsonTools.getJsonPathListMap( comments, "comments.data" );

        try {
            for (Map<String, Object> data : dataList) {
                String commentFromId = JsonTools.getJsonPathValue(data, "from.id", "");

                // comment from
                append(id, postId, "comments", lv2, commentFromId);
                commentsCounter.add( commentFromId );

                try {
                    List<Map<String, Object>> commentLikes = JsonTools.getJsonPathListMap(data, "likes.data");
                    for (Map<String, Object> commentLike : commentLikes) {
                        String likeFromId = JsonTools.getJsonPathValue(commentLike, "id", "");
                        append(id, postId, "likes", LV2_LIKES_COMMON, likeFromId);
                        likesCounter.add(likeFromId);

                        if( likeFromId.equals(id) ){
                            replayCounter.add(likeFromId);
                        }
                    }
                } catch (Exception ex) {
                }


                // try parse comments comment
                if ( LV2_COMMMON_FROM.equals(lv2))
                    procComments(data, id, postId, LV2_SUBCOMMMON_FROM);
            }
        }catch(Exception ex){

        }
        stdoutFlush();


        // paging next
        if ( LV2_COMMMON_FROM.equals(lv2)){
            String next = JsonTools.getJsonPathValue(comments, "comments.paging.next", "NotFound");

            if (! "NotFound".equals(next) ){
                procComments(loadPostCommentsAfter( next ), id, postId, LV2_COMMMON_FROM);
            }
        }
    }

    private void procLikes(Map<String, Object> likes, String id, String postId) {
        List<Map<String, Object>> dataList = JsonTools.getJsonPathListMap( likes, "likes.data" );
        System.err.println("likes size: " + dataList.size() );
        try {
            for (Map<String, Object> data : dataList) {
                String like_id = JsonTools.getJsonPathValue(data, "id", "");
                if (! "".equals(like_id) ){
                    append(id,postId,"likes", LV2_LIKES_POST, like_id);
                    likesCounter.add(like_id);
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }

        String next = JsonTools.getJsonPathValue(likes, "likes.paging.next", "NotFound");

        System.err.println(next);

        if (! "NotFound".equals(next)  ){
            procLikes(loadPostLikesAfter(next), id, postId);
        }
    }


    private void append(String id, String postId, String lv1, String lv2, String from){
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
