package co.gibar.crawler;

import co.gibar.crawler.exceptions.FBAccessTokenExpireException;

/**
 * Created by Abola Lee on 2015/8/29.
 */
public class FBCrawler extends WebCrawler{

    private String accessToken;

    private String clientId;
    private String clientSecret;

    private String apiVersion;


    @Override
    public String crawl(String target){

        try{
            getGraphApi(target);
        }catch( FBAccessTokenExpireException ex ){
            getAccessToken();
            return crawl(target);
        }


        return "";
    }

    /**
     * 取得 Facebook API access token
     * @return
     */
    protected String getAccessToken(){
        // https://graph.facebook.com/v2.4/oauth/access_token?client_id=626465174161774&client_secret=dbd550847406c13cd1da4085331ab54e&grant_type=client_credentials

        return "";
    }


    protected String getGraphApi(String api) throws FBAccessTokenExpireException {
        return "";
    }
}
