package co.gibar.crawler;

import java.util.List;

/**
 *
 *
 * @author Abola Lee<Abola921@gmail.com>
 */
public interface Crawler {


    String crawl(String target);


    String getLastError();

    List<String> getError();
}
