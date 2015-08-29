package co.gibar.crawler;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by Abola Lee on 2015/8/29.
 */
abstract public class AbstractCrawler implements Crawler {

    private List<String> error = Lists.newArrayList();

    public void addError(String errorLog){
        error.add(errorLog);
    }

    @Override
    public List<String> getError(){
        return this.error;
    }

    @Override
    public String getLastError(){
        return error.size()>0?
                error.get( error.size()-1 ) :
                null;
    }



}