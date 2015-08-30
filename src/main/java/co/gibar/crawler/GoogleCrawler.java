package co.gibar.crawler;

/**
 * Created by abola on 2015/8/30.
 */
public class GoogleCrawler extends WebCrawler {


    private String apiKey ;


    public GoogleCrawler(String apiKey){
        this.apiKey = apiKey;
    }

    @Override
    public String crawl(String target) {

        return "";
    }


    public static void main(String[] args){

//        new GoogleCrawler("123").crawl();
    }
}
