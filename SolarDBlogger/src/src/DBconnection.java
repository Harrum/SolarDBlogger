package src;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;


public class DBconnection 
{	
	final static String TABLE_NAME = "solar_values";
	/**
     * Connect to a sample database
     *
     * @param fileName the database file name
     */
    public static void createNewDatabase(String dbName) 
    {
        String url = "jdbc:sqlite:" + dbName;
 
        try (Connection conn = DriverManager.getConnection(url)) 
        {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } 
        catch (SQLException e) 
        {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Create a new table in the test database
     *
     */
    public static void createNewTable(String dbName) 
    {
        // SQLite connection string
        String url = "jdbc:sqlite:" + dbName;
        
        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	totalpac long NOT NULL,\n"
                + "	etoday long NOT NULL,\n"
                + "	etotal long NOT NULL,\n"
                + "	datetime long NOT NULL\n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
            System.out.println("Table created.");
        } 
        catch (SQLException e) 
        {
            System.out.println(e.getMessage());
        }
    }
    
    public static void deleteTable(String dbName)
    {
    	String url = "jdbc:sqlite:" + dbName;
    	
    	String sql = "DROP TABLE IF EXISTS solar_values";
    	
    	try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
            System.out.println("Table deleted.");
        } 
        catch (SQLException e) 
        {
            System.out.println(e.getMessage());
        }
    }
    
    public static void insert(String dbName, long totalPac, long eToday, long eTotal, long timeStamp) 
    {
    	String url = "jdbc:sqlite:" + dbName;
        String sql = "INSERT INTO " + TABLE_NAME + "(totalpac,etoday,etotal,datetime) VALUES(?,?,?,?)";
 
        try (Connection conn = DriverManager.getConnection(url);
        		PreparedStatement pstmt = conn.prepareStatement(sql)) {
        	pstmt.setLong(1, totalPac);
            pstmt.setLong(2, eToday);
            pstmt.setLong(3, eTotal);
            pstmt.setLong(4, timeStamp);
            pstmt.executeUpdate();
        } 
        catch (SQLException e) 
        {
            System.out.println(e.getMessage());
        }
    }
}
