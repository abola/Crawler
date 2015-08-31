package co.gibar.travel;

import co.gibar.crawler.Crawler;
import co.gibar.crawler.FBCrawler;
import co.gibar.crawler.JsonTools;
import co.gibar.datasource.MySQLDataSource;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by abola on 2015/8/30.
 */
public class DailyTracker {

    private Crawler crawl ;

    private String clientId;
    private String clientSecret;

    private String longToken;


    public DailyTracker(){
        loadConfiguration();

        if ( null !=  longToken && !"".equals(longToken))
            crawl = new FBCrawler(longToken);
        else
            crawl = new FBCrawler(clientId, clientSecret);
    }

    public void loadConfiguration(){
        String sqlLoadConfiguration = "select * from `configuration` where `key` in ('client_id','client_secret','long_token')";
        try {
            List<Map<String, Object>> results = MySQLDataSource.executeQuery(sqlLoadConfiguration, MySQLDataSource.connectToGibarCoDB);

            for( Map<String, Object> setting: results ){
                if ( "client_id".equals(setting.get("key").toString()) ) this.clientId = setting.get("value").toString();
                if ( "client_secret".equals(setting.get("key").toString()) ) this.clientSecret = setting.get("value").toString();
                if ( "long_token".equals(setting.get("key").toString()) ) this.longToken = setting.get("value").toString();
            }
        }catch(Exception ex){
            // throw ConfigurationException
        }
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

    public Map<String, Object> callGraphAPI(String id){

        List<Map<String,Object>> jsonResult = crawl.crawlJson(id + "?fields=id,name,location,general_info,likes,link,checkins,cover,category,category_list,website,description,talking_about_count");

        return jsonResult.get(0);
    }

    public void updateAll(List<Map<String, Object>> resultList){
        List<String> executeSql = Lists.newArrayList();
        for( Map<String, Object> result : resultList){

            String id  = result.get("id").toString();
            String name = result.get("name").toString();
            String link = result.get("link").toString();
            String category = JsonTools.getJsonPathValue(result, "category", "");

            // option
            String likes = JsonTools.getJsonPathValue(result, "likes","0");
            String lat = JsonTools.getJsonPathValue(result, "location.latitude","0");
            String lng = JsonTools.getJsonPathValue(result, "location.longitude","0");

            String cover = JsonTools.getJsonPathValue(result, "cover","");
            String checkins = JsonTools.getJsonPathValue(result, "checkins","0");
            String talking_about_count = JsonTools.getJsonPathValue(result, "talking_about_count","0");
            String website = JsonTools.getJsonPathValue(result, "website","");

            String insertOrUpdatePage =
                    "insert into `page`(id,name,lat,lng,link,cover,category,description,website) " +
                    "values("+id+",'"+name+"',"+lat+","+lng+",'"+link+"','"+cover+"','"+category+"','','"+website+"') " +
                    "on duplicate key update name=values(name), lat=values(lat), lng=values(lng)" +
                    " , link=values(link), cover=values(cover), category=values(category) " +
                    " , description=values(description), website=values(website);";

            String insertOrUpdatePageVolume =
                    "insert into `page_volume_count`(id,date,likes,talking_about_count,checkins) " +
                    "values("+id+",DATE(now()),"+likes+","+talking_about_count+","+checkins+") " +
                    "on duplicate key update likes=values(likes), talking_about_count=values(talking_about_count), checkins=values(checkins) ;" ;

            String updateRegisterTable =
                    "update `register_table` set last_update =now() where `alias` = '"+id+"' ; " ;


            if ( null != result.get("category_list") ){
                List categoryList = (ArrayList) result.get("category_list") ;

                for( Map<String, Object> subCategory: (List<Map<String, Object>>) categoryList){
                    String subCategoryId = JsonTools.getJsonPathValue(subCategory, "id","");
                    String subCategoryName = JsonTools.getJsonPathValue(subCategory, "name","");

                    if ("" . equals( subCategoryId )) continue;


                    String insertOrUpdatePageCategory =
                            "insert into `page_category`(id,category,name,last_update) " +
                            "values("+id+","+subCategoryId+",'"+subCategoryName+"',DATE(now()) ) " +
                            "on duplicate key update name=values(name), last_update=values(last_update);";

                    executeSql.add(insertOrUpdatePageCategory);
                }
                System.out.println(categoryList.toString());

            }
            executeSql.add(insertOrUpdatePage);
            executeSql.add(insertOrUpdatePageVolume);
            executeSql.add(updateRegisterTable);
        }

        try {
            MySQLDataSource.execute( executeSql, MySQLDataSource.connectToGibarCoDB );
        }catch(Exception ex){
            ex.printStackTrace();
        }

    }

    public List<String> getRegisterList(){
        String sqlLoadRegisterList = "select `alias` from `register_table` where `suspend` = 0 and last_update < DATE(now()) limit 30  ";

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
