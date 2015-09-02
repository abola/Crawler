package co.gibar.datasource;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataSource implements DataSource {

    Logger log = Logger.getLogger( this.getClass() );

    Connection connection = null;

    @Override public void connect() throws DataSourceException {
        try{
            Class.forName( getDriver() );
            connection = DriverManager.getConnection( getConnectionString() );
        }
        catch(Exception e){
            log.error("Connection failed. CauseBy: " + e.getMessage()  );
            e.printStackTrace();
            throw new DataSourceException( e.getMessage() );
        }
    }

    @Override public void close(){
        try {
            this.connection.close();
        } catch (SQLException e) {
            // close anyway
        }
    }

    @Override public List<Map<String, Object>> query(String sql) throws DataSourceException{

        if ( null == connection ) {
            connect();
            return query(sql);
        }

        Statement stmt = null ;
        try {
            stmt = connection.createStatement();
        } catch (SQLException e) {
            log.error("Create statement failed CauseBy: " + e.getMessage()  );
            e.printStackTrace();
            throw new DataSourceException( e.getMessage() );
        }

        ResultSet rs = null ;
        List<Map<String, Object>> result = null;
        try {
            rs = stmt.executeQuery( sql );	// execute
            result = resultSetToListMap(rs);// transform to List<Map>
        } catch (SQLException e) {
            log.error("SQL: " + sql );
            log.error("Query failed. CauseBy: " + e.getMessage() );
            e.printStackTrace();
            throw new DataSourceException( e.getMessage() );
        }

        // release resource
        try {
            rs.close(); rs=null;
            stmt.close(); stmt=null;
            close();connection=null;
        } catch (Exception e) {
            // close anyway
        }


        return result;
    }

    @Override public void execute(String sql )throws DataSourceException{
        if ( null == connection ) {
            connect();
            execute(sql);
            return;
        }
        Statement stmt = null ;
        try {
            stmt = connection.createStatement();
        } catch (SQLException e) {
            log.error("Create statement failed CauseBy: " + e.getMessage()  );
            e.printStackTrace();
            throw new DataSourceException( e.getMessage() );
        }

        try {
            stmt.execute(sql);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        // release resource
        try {
            stmt.close(); stmt=null;
            close(); connection=null;
        } catch (Exception e) {
            // close anyway
        }
    }

    @Override public void execute(Collection<String> sql )throws DataSourceException{
        if ( null == connection ) {
            connect();
            execute(sql);
            return;
        }
        Statement stmt = null ;
        try {
            stmt = connection.createStatement();
        } catch (SQLException e) {
            log.error("Create statement failed CauseBy: " + e.getMessage()  );
            e.printStackTrace();
            throw new DataSourceException( e.getMessage() );
        }

        String _sql = "";
        try {
            for( String eachSQL: sql ){
                _sql = eachSQL;
                stmt.execute(eachSQL);
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
            System.out.println(_sql);
            throw new DataSourceException(e1.getMessage() + ". SQL: " + _sql);
        }

        // release resource
        try {
            stmt.close(); stmt=null;
            close(); connection=null;
        } catch (Exception e) {
            // close anyway
        }
    }

    /**
     * 取得 DB 連線使用的 Driver
     * @return
     */
    protected abstract String getDriver();

    /**
     * Extend 的類別需要設定 ConnectionString
     * @return
     */
    protected abstract String getConnectionString();

    protected abstract void setConnectionString(String connectionString);

    /**
     * 將 ResultSet 轉為 ListMap
     * @param rs
     * @return
     * @throws SQLException
     */
    public List<Map<String, Object>> resultSetToListMap(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        ImmutableList.Builder<Map<String, Object>> builder = ImmutableList.builder();

        while (rs.next()) {
            ImmutableMap.Builder<String, Object> mapBuilder =  ImmutableMap.builder();

            for (int i = 1; i <= columns; ++i) {
                Object v = null;
                try{
                    v = (null==rs.getObject(i)?"":rs.getObject(i));
                }
                catch(java.sql.SQLException e1){
                    try{
                        v = rs.getString(i);
                    }catch(java.sql.SQLException e2){
                        v = "";
                    }
                }
                mapBuilder.put(md.getColumnName(i), v );
            }

            builder.add( mapBuilder.build() );
        }

        return builder.build();
    }
}
