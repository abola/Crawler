package co.gibar.travel;

import co.gibar.crawler.Crawler;
import co.gibar.crawler.FBCrawler;
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

        StoreTools.startStdoutTo("/tmp/testme");

        System.out.append(this.clientId);
        System.out.append(this.clientSecret);

        System.out.flush();

        StoreTools.resetStdout();

        System.out.append(this.clientId);
        System.out.append(this.clientSecret);


        System.out.flush();
        return this;
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
