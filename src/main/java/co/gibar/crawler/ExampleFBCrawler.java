package co.gibar.crawler;

/**
 * Created by abola on 2015/8/30.
 */
public class ExampleFBCrawler {


    public void runExample(){
        String clientId = "626465174161774";
        String clientSecret = "dbd550847406c13cd1da4085331ab54e";

        String responseJson = new FBCrawler(clientId, clientSecret).crawl("XDSelect?fields=id,name,location,general_info,likes,link,checkins,posts,picture,cover");

        System.out.println(responseJson);
    }

    public static void main(String[] args){
        new ExampleFBCrawler().runExample();
    }

}
