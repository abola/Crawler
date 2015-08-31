package co.gibar.crawler;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author Abola Lee<Abola921@gmail.com>
 */
public interface Crawler {


    /**
     *
     * @param target
     * @return
     */
    String crawl(String target);


    List<Map<String, Object>> crawlJson(String target);

    String getLastError();

    List<String> getError();
}
