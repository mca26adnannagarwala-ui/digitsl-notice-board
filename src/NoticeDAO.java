import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoticeDAO {

    public String[] authenticateUser(String username, String password, String requiredRole) throws SQLException {
        String sql = "SELECT full_name, role FROM users WHERE username = ? AND password = ? AND role = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, requiredRole);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString("full_name"), rs.getString("role")};
                }
            }
        }
        return null;
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, username);
            ps.setString(3, oldPassword);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> getActiveNotices() {
        return fetchNoticesByCondition("expiry_date >= CURDATE()");
    }

    public List<Object[]> getExpiredNotices() {
        return fetchNoticesByCondition("expiry_date < CURDATE()");
    }

    private List<Object[]> fetchNoticesByCondition(String condition) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, title, category, priority, posted_by, expiry_date, description " +
                     "FROM notices WHERE " + condition + " " +
                     "ORDER BY FIELD(priority, 'Urgent', 'Normal'), created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("priority"),
                    rs.getString("posted_by"),
                    rs.getDate("expiry_date").toString(),
                    rs.getString("description")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int[] getDashboardStats() {
        int[] stats = new int[3];
        String sql = "SELECT " +
                     "COUNT(*) AS total_active, " +
                     "SUM(CASE WHEN priority = 'Urgent' THEN 1 ELSE 0 END) AS urgent_count, " +
                     "SUM(CASE WHEN expiry_date = CURDATE() THEN 1 ELSE 0 END) AS expiring_today " +
                     "FROM notices WHERE expiry_date >= CURDATE()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats[0] = rs.getInt("total_active");
                stats[1] = rs.getInt("urgent_count");
                stats[2] = rs.getInt("expiring_today");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public boolean addNotice(String title, String desc, String category, String priority, String postedBy, String expiryDate) {
        String sql = "INSERT INTO notices (title, description, category, priority, posted_by, expiry_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, desc);
            ps.setString(3, category);
            ps.setString(4, priority);
            ps.setString(5, postedBy);
            ps.setDate(6, Date.valueOf(expiryDate));
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateNotice(int id, String title, String desc, String category, String priority, String expiryDate, String postedBy) {
        String sql = "UPDATE notices SET title = ?, description = ?, category = ?, priority = ?, expiry_date = ? WHERE id = ? AND posted_by = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, desc);
            ps.setString(3, category);
            ps.setString(4, priority);
            ps.setDate(5, Date.valueOf(expiryDate));
            ps.setInt(6, id);
            ps.setString(7, postedBy);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteNotice(int id, String postedBy) {
        String sql = "DELETE FROM notices WHERE id = ? AND posted_by = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, postedBy);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}