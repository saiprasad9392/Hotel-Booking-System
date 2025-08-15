import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/hotel_booking";
    private static final String USER = "root"; // Your MySQL user
    private static final String PASSWORD = "Sai@9502"; // Your MySQL password

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
