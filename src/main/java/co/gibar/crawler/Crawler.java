package co.gibar.crawler;

import com.google.common.collect.ImmutableMap;

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


    List<ImmutableMap<String, Object>> crawlJson(String target);

    String getLastError();

    List<String> getError();
}
