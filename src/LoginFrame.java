import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JToggleButton btnStudentRole, btnProfRole;
    private NoticeDAO noticeDAO = new NoticeDAO();

    public LoginFrame() {
        setTitle("GNIMS - Digital Notice Board Login");
        setSize(940, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainContainer = new JPanel(new GridLayout(1, 2));

        // LEFT PANEL: Modern Gradient Branding Showcase
        GradientPanel leftPanel = new GradientPanel(new Color(15, 23, 42), new Color(37, 99, 235));
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setBorder(new EmptyBorder(45, 42, 45, 42));

        JPanel brandTop = new JPanel();
        brandTop.setOpaque(false);
        brandTop.setLayout(new BoxLayout(brandTop, BoxLayout.Y_AXIS));

        JLabel badge = new JLabel("<html>&nbsp;GURU NANAK INSTITUTE OF<br>&nbsp;MANAGEMENT STUDIES&nbsp;</html>");
        badge.setOpaque(true);
        badge.setBackground(new Color(59, 130, 246));
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel("<html>Digital Notice<br>Board System</html>");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setBorder(new EmptyBorder(20, 0, 12, 0));

        JLabel subtitle = new JLabel("<html>Centralized campus announcements, real-time<br>urgent alerts, and automated notice lifecycle<br>management for students & faculty.</html>");
        subtitle.setForeground(new Color(203, 213, 225));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        brandTop.add(badge);
        brandTop.add(title);
        brandTop.add(subtitle);

        leftPanel.add(brandTop, BorderLayout.NORTH);

        // RIGHT PANEL: Clean White Login Card
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(248, 250, 252));

        RoundedPanel loginCard = new RoundedPanel(20, Color.WHITE);
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setBorder(new EmptyBorder(36, 34, 36, 34));
        loginCard.setPreferredSize(new Dimension(390, 415));

        JLabel welcomeLbl = new JLabel("Welcome Back");
        welcomeLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeLbl.setForeground(new Color(30, 41, 59));
        welcomeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel("Sign in using your college-issued credentials");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLbl.setForeground(new Color(100, 116, 139));
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Role Selector Toggle Pills
        JPanel rolePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        rolePanel.setOpaque(false);
        rolePanel.setMaximumSize(new Dimension(330, 40));
        rolePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnStudentRole = createRoleToggle("Student", true);
        btnProfRole = createRoleToggle("Professor", false);

        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(btnStudentRole);
        roleGroup.add(btnProfRole);

        btnStudentRole.addActionListener(e -> updateRoleStyles());
        btnProfRole.addActionListener(e -> updateRoleStyles());

        rolePanel.add(btnStudentRole);
        rolePanel.add(btnProfRole);

        // Username Input
        JLabel lblUser = createFieldLabel("College Email / Username");
        txtUsername = createStyledTextField();

        // Password Input
        JLabel lblPass = createFieldLabel("Password");
        txtPassword = new JPasswordField();
        styleInputBox(txtPassword);

        // Enter-Key Login Support
        ActionListener enterSubmit = e -> handleLogin();
        txtUsername.addActionListener(enterSubmit);
        txtPassword.addActionListener(enterSubmit);

        // Sign In Button
        ModernButton btnLogin = new ModernButton("Sign In to Portal", new Color(37, 99, 235), new Color(29, 78, 216));
        btnLogin.setMaximumSize(new Dimension(330, 44));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(enterSubmit);

        loginCard.add(welcomeLbl);
        loginCard.add(Box.createVerticalStrut(4));
        loginCard.add(subLbl);
        loginCard.add(Box.createVerticalStrut(20));
        loginCard.add(rolePanel);
        loginCard.add(Box.createVerticalStrut(20));
        loginCard.add(lblUser);
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(txtUsername);
        loginCard.add(Box.createVerticalStrut(16));
        loginCard.add(lblPass);
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(txtPassword);
        loginCard.add(Box.createVerticalStrut(26));
        loginCard.add(btnLogin);

        rightPanel.add(loginCard);

        mainContainer.add(leftPanel);
        mainContainer.add(rightPanel);
        add(mainContainer);
    }

    private void handleLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        String role = btnProfRole.isSelected() ? "PROFESSOR" : "STUDENT";

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String[] userData = noticeDAO.authenticateUser(user, pass, role);
            if (userData != null) {
                dispose();
                new MainDashboard(user, userData[0], "PROFESSOR".equals(userData[1])).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials for selected role (" + role + ").", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            String details = e.getMessage();
            if (e.getSQLState() != null) {
                details += " (SQLState: " + e.getSQLState() + ")";
            }
            JOptionPane.showMessageDialog(this, "Database error: " + details, "Database Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JToggleButton createRoleToggle(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true));
        return btn;
    }

    private void updateRoleStyles() {
        if (btnStudentRole.isSelected()) {
            btnStudentRole.setBackground(new Color(37, 99, 235));
            btnStudentRole.setForeground(Color.WHITE);
            btnProfRole.setBackground(new Color(241, 245, 249));
            btnProfRole.setForeground(new Color(71, 85, 105));
        } else {
            btnProfRole.setBackground(new Color(37, 99, 235));
            btnProfRole.setForeground(Color.WHITE);
            btnStudentRole.setBackground(new Color(241, 245, 249));
            btnStudentRole.setForeground(new Color(71, 85, 105));
        }
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        styleInputBox(tf);
        return tf;
    }

    private void styleInputBox(JComponent field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(340, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.updateRoleStyles();
            frame.setVisible(true);
        });
    }

    static class GradientPanel extends JPanel {
        private Color c1, c2;
        GradientPanel(Color c1, Color c2) { this.c1 = c1; this.c2 = c2; }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    static class RoundedPanel extends JPanel {
        private int radius;
        private Color bg;
        private Color borderColor;

        RoundedPanel(int radius, Color bg) {
            this(radius, bg, new Color(226, 232, 240));
        }

        RoundedPanel(int radius, Color bg, Color borderColor) {
            this.radius = radius;
            this.bg = bg;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radius, radius));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ModernButton extends JButton {
        private Color currentBg;

        ModernButton(String text, Color normalBg, Color hoverBg) {
            super(text);
            this.currentBg = normalBg;
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { currentBg = hoverBg; repaint(); }
                @Override public void mouseExited(MouseEvent e) { currentBg = normalBg; repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(currentBg);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            g2.dispose();
            super.paintComponent(g);
        }
    }
}