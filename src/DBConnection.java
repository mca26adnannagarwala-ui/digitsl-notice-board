import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static volatile boolean initialized = false;

    public static Connection getConnection() throws SQLException {
        String host = environmentOrDefault("DB_HOST", "mysql-2d17dc32-digital-notice-board.f.aivencloud.com");
        String port = environmentOrDefault("DB_PORT", "18125");
        String db = environmentOrDefault("DB_NAME", "defaultdb");
        String user = environmentOrDefault("DB_USER", "avnadmin");
        String pass = requiredEnvironment("DB_PASS");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                   + "?useSSL=true&requireSSL=true&verifyServerCertificate=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        Connection conn = DriverManager.getConnection(url, user, pass);
        try {
            synchronized (DBConnection.class) {
                if (!initialized) {
                    setupTables(conn);
                    initialized = true;
                }
            }
            return conn;
        } catch (SQLException e) {
            try {
                conn.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String requiredEnvironment(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SQLException("Required environment variable " + name + " is not set.");
        }
        return value;
    }

    private static void setupTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    full_name VARCHAR(150) NOT NULL,
                    role VARCHAR(20) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notices (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description TEXT NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    priority VARCHAR(50) NOT NULL,
                    posted_by VARCHAR(150) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expiry_date DATE NOT NULL
                )
            """);

            try (PreparedStatement userSeed = conn.prepareStatement(
                    "INSERT IGNORE INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)")) {
                userSeed.setString(1, "ifrah.kampoo@gnims.edu.in");
                userSeed.setString(2, requiredEnvironment("DB_SEED_PROFESSOR_PASSWORD"));
                userSeed.setString(3, "Prof. Ifrah Kampoo (Advanced Java)");
                userSeed.setString(4, "PROFESSOR");
                userSeed.addBatch();

                userSeed.setString(1, "adnan.nagarwala@gnims.edu.in");
                userSeed.setString(2, requiredEnvironment("DB_SEED_STUDENT_PASSWORD"));
                userSeed.setString(3, "Adnan Nagarwala");
                userSeed.setString(4, "STUDENT");
                userSeed.addBatch();
                userSeed.executeBatch();
            }

            stmt.execute("""
                INSERT IGNORE INTO notices
                    (id, title, description, category, priority, posted_by, expiry_date)
                VALUES
                    (1, 'Exam Schedule', 'Exam is from 20 nov', 'Exams', 'Urgent',
                     'Prof. Ifrah Kampoo (Advanced Java)', DATE_ADD(CURDATE(), INTERVAL 30 DAY))
            """);
        }
    }
}