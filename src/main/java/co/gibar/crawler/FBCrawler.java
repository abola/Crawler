package co.gibar.crawler;

import co.gibar.crawler.exceptions.FBAccessTokenExpireException;

/**
 * Created by Abola Lee on 2015/8/29.
 */
public class FBCrawler extends WebCrawler{

    private String accessToken;

    private String clientId;
    private String clientSecret;

    private String apiVersion = "v2.4";


    public FBCrawler(String clientId, String clientSecret){
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String crawl(String target){

        if (null == accessToken)
            this.accessToken = getAccessToken();
        try{
            return getGraphApi(target);
        }catch( FBAccessTokenExpireException ex ){
            this.accessToken = null;
            return crawl(target);
        }
    }

    /**
     * 取得 Facebook API access token
     * @return
     */
    protected String getAccessToken(){
        //
        String requestAccessTokenUrl = "https://graph.facebook.com" +
                "/oauth/access_token" +
                "?client_id=" + this.clientId +
                "&client_secret=" + this.clientSecret +
                "&grant_type=client_credentials";
        String response = getUrl(requestAccessTokenUrl);


//        System.out.println(response);
        return response.split("=")[1];
    }

    protected String getGraphApi(String api) throws FBAccessTokenExpireException {
        String request = "https://graph.facebook.com/" +
                this.apiVersion + "/" +
                api +
                "&locale=zh_TW" +
                "&access_token=" + this.accessToken;
        return getUrl( request ) ;
    }

//
//    public static void main(String args[]){
////        System.out.print("start...");
//        System.out.println(
//                new FBCrawler("626465174161774", "dbd550847406c13cd1da4085331ab54e")
//                .crawl("XDSelect?fields=id,name")
//        );
//    }
}
