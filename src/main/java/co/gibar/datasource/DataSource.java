package co.gibar.datasource;


import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DataSource {

    /**
     * 與資料庫進行連線
     */
    void connect() throws DataSourceException;


    /**
     * 關閉連線
     */
    void close();

    /**
     * 查詢資料並取得部份內容
     * @param sql
     * @return
     * @throws DataSourceException
     */
    List<Map<String, Object>> query(String sql) throws DataSourceException;



    /**
     * 執行sql update/delete/insert
     * @param sql
     * @throws DataSourceException
     */
    void execute(String sql) throws DataSourceException;

    /**
     * 執行sql update/delete/insert
     * @param sql
     * @throws DataSourceException
     */
    void execute(Collection<String> sql) throws DataSourceException;
}
