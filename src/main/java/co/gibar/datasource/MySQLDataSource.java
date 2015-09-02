package co.gibar.datasource;


import java.util.List;
import java.util.Map;

public class MySQLDataSource extends AbstractDataSource {

    static String driver  = "com.mysql.jdbc.Driver";


    public static final String connectToGibarCoDB =
            "jdbc:mysql://128.199.204.20/fb?user=fb&password=&useUnicode=true&characterEncoding=utf8";


    // default
    public String connectionString = connectToGibarCoDB;

    @Override protected String getDriver(){
        return driver;
    }

    protected String getConnectionString() {
        return connectionString;
    }

    @Override protected void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }



    public static List<Map<String, Object>> executeQuery(String sql) throws DataSourceException{
        return new MySQLDataSource().query(sql);
    }

    public static List<Map<String, Object>> executeQuery(String sql, String connectionString ) throws DataSourceException{
        MySQLDataSource mds = new MySQLDataSource();
        mds.setConnectionString( connectionString );
        mds.execute("SET NAMES utf8mb4;");
        return mds.query(sql);
    }


    public static void execute(String sql, String connectionString ) throws DataSourceException{
        MySQLDataSource mds = new MySQLDataSource();
        mds.setConnectionString(connectionString);
        mds.execute("SET NAMES utf8mb4;");
        mds.execute(sql);
    }

    public static void execute(List<String> sql, String connectionString ) throws DataSourceException{
        MySQLDataSource mds = new MySQLDataSource();
        mds.setConnectionString( connectionString);
        mds.execute("SET NAMES utf8mb4;");
        mds.execute(sql);
    }
}
