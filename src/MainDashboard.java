import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MainDashboard extends JFrame {
    private String currentUsername;
    private String currentUserName;
    private String professorSubject;
    private boolean isProfessor;
    private NoticeDAO noticeDAO = new NoticeDAO();

    private JTable noticeTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private JLabel lblTotalCount, lblUrgentCount, lblExpiringCount;
    private JLabel lblLiveClock;
    private JTextField txtSearch;
    private JComboBox<String> cmbCategoryFilter, cmbSubjectFilter;
    private final String SEARCH_PLACEHOLDER = "Search title, keyword... (Ctrl+F)";

    // Live Urgent Alert Banner & Tracking
    private JPanel urgentAlertBanner;
    private JLabel lblUrgentBannerText;
    private int lastKnownUrgentCount = -1;

    // Live Notice Reader Card Components
    private JLabel lblPreviewTitle, lblPreviewMeta, lblPreviewBadge;
    private JTextArea txtPreviewBody;

    // Professor Admin Edit/Create & Archive State
    private Integer editingNoticeId = null;
    private boolean showingExpiredArchive = false;
    private JLabel lblAdminFormHeader, lblAdminFormSub;
    private JTextField txtAdminTitle, txtAdminExpiry;
    private JComboBox<String> cmbAdminCategory, cmbAdminPriority;
    private JTextArea txtAdminDesc;
    private LoginFrame.ModernButton btnAdminSubmit, btnToggleArchive;

    // Custom Pill Tab Switcher Components
    private CardLayout contentCardLayout;
    private JPanel contentCardsPanel;
    private JButton btnTabFeed, btnTabAdmin;

    private Timer clockTimer, autoSyncTimer;

    public MainDashboard(String username, String fullName, boolean isProfessor) {
        this.currentUsername = username;
        this.currentUserName = fullName;
        this.isProfessor = isProfessor;
        this.professorSubject = resolveProfessorSubject(username, fullName);

        setTitle("GNIMS Digital Notice Board - " + (isProfessor ? "Professor Admin Portal" : "Student Portal"));
        setSize(1210, 745);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout(12, 12));
        rootPanel.setBackground(new Color(241, 245, 249));
        rootPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Ctrl+F Keyboard Shortcut to Focus Search Bar
        rootPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch"
        );
        rootPanel.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                contentCardLayout.show(contentCardsPanel, "FEED");
                updateNavPillStyle(btnTabFeed, true);
                if (btnTabAdmin != null) updateNavPillStyle(btnTabAdmin, false);
                txtSearch.requestFocusInWindow();
            }
        });

        // 1. Top Header + Custom Pill Navigation Bar
        JPanel topWrapper = new JPanel(new BorderLayout(0, 12));
        topWrapper.setOpaque(false);
        topWrapper.add(createTopBar(), BorderLayout.NORTH);
        topWrapper.add(createPillNavBar(), BorderLayout.SOUTH);
        rootPanel.add(topWrapper, BorderLayout.NORTH);

        // 2. CardLayout Content Area
        contentCardLayout = new CardLayout();
        contentCardsPanel = new JPanel(contentCardLayout);
        contentCardsPanel.setOpaque(false);

        contentCardsPanel.add(createNoticeFeedPanel(), "FEED");
        if (isProfessor) {
            contentCardsPanel.add(createProfessorAdminPanel(), "ADMIN");
        }

        rootPanel.add(contentCardsPanel, BorderLayout.CENTER);
        add(rootPanel);

        loadDashboardData();
        startBackgroundTimers();
    }

    private String resolveProfessorSubject(String username, String fullName) {
        if (!isProfessor) return "";
        String key = (username + " " + fullName).toLowerCase();
        if (key.contains("ifrah")) return "Advanced Java";
        if (key.contains("mansi")) return "Software Project Management";
        if (key.contains("lohita")) return "Digital Marketing";
        return "Faculty";
    }

    private String getFormattedAuthorWithSubject() {
        if (professorSubject != null && !professorSubject.isEmpty()) {
            return currentUserName + " (" + professorSubject + ")";
        }
        return currentUserName;
    }

    private void startBackgroundTimers() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy  •  hh:mm:ss a");
        lblLiveClock.setText(sdf.format(new Date()));
        clockTimer = new Timer(1000, e -> lblLiveClock.setText(sdf.format(new Date())));
        clockTimer.start();

        autoSyncTimer = new Timer(15000, e -> {
            if (showingExpiredArchive) return;
            int selectedRow = noticeTable.getSelectedRow();
            Integer selectedId = null;
            if (selectedRow != -1) {
                int modelRow = noticeTable.convertRowIndexToModel(selectedRow);
                selectedId = (Integer) tableModel.getValueAt(modelRow, 0);
            }
            loadDashboardData();
            if (selectedId != null) {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (selectedId.equals(tableModel.getValueAt(i, 0))) {
                        int viewRow = noticeTable.convertRowIndexToView(i);
                        if (viewRow != -1) noticeTable.setRowSelectionInterval(viewRow, viewRow);
                        break;
                    }
                }
            }
        });
        autoSyncTimer.start();
    }

    @Override
    public void dispose() {
        if (clockTimer != null) clockTimer.stop();
        if (autoSyncTimer != null) autoSyncTimer.stop();
        super.dispose();
    }

    private JPanel createTopBar() {
        LoginFrame.RoundedPanel topBar = new LoginFrame.RoundedPanel(16, new Color(30, 41, 59), null);
        topBar.setLayout(new BorderLayout());
        topBar.setBorder(new EmptyBorder(12, 22, 12, 22));

        JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftInfo.setOpaque(false);

        JPanel brandStack = new JPanel(new GridLayout(2, 1, 0, 2));
        brandStack.setOpaque(false);

        JLabel lblInstitute = new JLabel("GURU NANAK INSTITUTE OF MANAGEMENT STUDIES");
        lblInstitute.setForeground(new Color(147, 197, 253));
        lblInstitute.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JLabel lblBrand = new JLabel("DIGITAL NOTICE BOARD");
        lblBrand.setForeground(Color.WHITE);
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 17));

        brandStack.add(lblInstitute);
        brandStack.add(lblBrand);

        JLabel lblBadge = new JLabel(isProfessor ? "  PROFESSOR ADMIN  " : "  STUDENT PORTAL  ");
        lblBadge.setOpaque(true);
        lblBadge.setBackground(isProfessor ? new Color(37, 99, 235) : new Color(16, 185, 129));
        lblBadge.setForeground(Color.WHITE);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setBorder(new EmptyBorder(4, 8, 4, 8));

        leftInfo.add(brandStack);
        leftInfo.add(lblBadge);

        if (isProfessor && !professorSubject.isEmpty()) {
            JLabel lblSubjectPill = new JLabel("  " + professorSubject.toUpperCase() + "  ");
            lblSubjectPill.setOpaque(true);
            lblSubjectPill.setBackground(new Color(51, 65, 85));
            lblSubjectPill.setForeground(new Color(147, 197, 253));
            lblSubjectPill.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblSubjectPill.setBorder(new EmptyBorder(4, 8, 4, 8));
            leftInfo.add(lblSubjectPill);
        }

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        rightInfo.setOpaque(false);

        lblLiveClock = new JLabel();
        lblLiveClock.setForeground(new Color(148, 163, 184));
        lblLiveClock.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblUser = new JLabel("Welcome, " + currentUserName);
        lblUser.setForeground(new Color(226, 232, 240));
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));

        LoginFrame.ModernButton btnChangePass = new LoginFrame.ModernButton("Change Password", new Color(51, 65, 85), new Color(71, 85, 105));
        btnChangePass.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnChangePass.setPreferredSize(new Dimension(130, 32));
        btnChangePass.addActionListener(e -> openChangePasswordDialog());

        LoginFrame.ModernButton btnLogout = new LoginFrame.ModernButton("Logout", new Color(220, 38, 38), new Color(185, 28, 28));
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setPreferredSize(new Dimension(84, 32));
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        rightInfo.add(lblLiveClock);
        rightInfo.add(lblUser);
        rightInfo.add(btnChangePass);
        rightInfo.add(btnLogout);

        topBar.add(leftInfo, BorderLayout.WEST);
        topBar.add(rightInfo, BorderLayout.EAST);
        return topBar;
    }

    private void openChangePasswordDialog() {
        JDialog dialog = new JDialog(this, "Change Account Password", true);
        dialog.setSize(390, 310);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel head = new JLabel("Update Password (" + currentUsername + ")");
        head.setFont(new Font("Segoe UI", Font.BOLD, 15));
        head.setForeground(new Color(30, 41, 59));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField txtOld = new JPasswordField();
        JPasswordField txtNew = new JPasswordField();
        stylePassDialogField(txtOld);
        stylePassDialogField(txtNew);

        LoginFrame.ModernButton btnSave = new LoginFrame.ModernButton("Update Password", new Color(37, 99, 235), new Color(29, 78, 216));
        btnSave.setMaximumSize(new Dimension(330, 38));
        btnSave.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSave.addActionListener(e -> {
            String oldP = new String(txtOld.getPassword()).trim();
            String newP = new String(txtNew.getPassword()).trim();
            if (oldP.isEmpty() || newP.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill both fields.", "Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (noticeDAO.changePassword(currentUsername, oldP, newP)) {
                JOptionPane.showMessageDialog(dialog, "Password updated successfully!");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Current password is incorrect.", "Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(head);
        panel.add(Box.createVerticalStrut(16));
        panel.add(createAdminLabel("Current Password"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtOld);
        panel.add(Box.createVerticalStrut(12));
        panel.add(createAdminLabel("New Password"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtNew);
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnSave);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void stylePassDialogField(JPasswordField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setMaximumSize(new Dimension(330, 36));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));
    }

    private JPanel createPillNavBar() {
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navBar.setOpaque(false);

        btnTabFeed = createNavPillButton("Active Notice Board", true);
        btnTabFeed.addActionListener(e -> {
            contentCardLayout.show(contentCardsPanel, "FEED");
            updateNavPillStyle(btnTabFeed, true);
            if (btnTabAdmin != null) updateNavPillStyle(btnTabAdmin, false);
        });
        navBar.add(btnTabFeed);

        if (isProfessor) {
            btnTabAdmin = createNavPillButton("Professor Admin Panel (" + professorSubject + ")", false);
            btnTabAdmin.addActionListener(e -> {
                contentCardLayout.show(contentCardsPanel, "ADMIN");
                updateNavPillStyle(btnTabAdmin, true);
                updateNavPillStyle(btnTabFeed, false);
            });
            navBar.add(btnTabAdmin);
        }

        return navBar;
    }

    private JButton createNavPillButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        updateNavPillStyle(btn, active);
        return btn;
    }

    private void updateNavPillStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(37, 99, 235));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(71, 85, 105));
        }
        btn.repaint();
    }

    private JPanel createNoticeFeedPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        // Dismissible New Urgent Notice Alert Banner
        urgentAlertBanner = new LoginFrame.RoundedPanel(12, new Color(254, 226, 226), new Color(248, 113, 113));
        urgentAlertBanner.setLayout(new BorderLayout());
        urgentAlertBanner.setBorder(new EmptyBorder(8, 16, 8, 12));
        urgentAlertBanner.setVisible(false);

        lblUrgentBannerText = new JLabel("NEW URGENT NOTICE PUBLISHED — Check the highlighted row below!");
        lblUrgentBannerText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUrgentBannerText.setForeground(new Color(185, 28, 28));

        JButton btnDismiss = new JButton("Dismiss");
        btnDismiss.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDismiss.setForeground(new Color(185, 28, 28));
        btnDismiss.setContentAreaFilled(false);
        btnDismiss.setBorderPainted(false);
        btnDismiss.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDismiss.addActionListener(e -> urgentAlertBanner.setVisible(false));

        urgentAlertBanner.add(lblUrgentBannerText, BorderLayout.WEST);
        urgentAlertBanner.add(btnDismiss, BorderLayout.EAST);

        // Analytics Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 18, 0));
        statsPanel.setOpaque(false);

        lblTotalCount = new JLabel("0");
        lblUrgentCount = new JLabel("0");
        lblExpiringCount = new JLabel("0");

        statsPanel.add(createMetricCard("TOTAL ACTIVE NOTICES", lblTotalCount, new Color(37, 99, 235), new Color(239, 246, 255)));
        statsPanel.add(createMetricCard("URGENT ALERTS", lblUrgentCount, new Color(220, 38, 38), new Color(254, 242, 242)));
        statsPanel.add(createMetricCard("EXPIRING TODAY", lblExpiringCount, new Color(217, 119, 6), new Color(255, 251, 235)));

        // Search & Filter Bar
        LoginFrame.RoundedPanel filterBar = new LoginFrame.RoundedPanel(14, Color.WHITE);
        filterBar.setLayout(new FlowLayout(FlowLayout.LEFT, 9, 10));
        filterBar.setBorder(new EmptyBorder(4, 12, 4, 12));

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(new Color(51, 65, 85));

        txtSearch = new JTextField(SEARCH_PLACEHOLDER, 18);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setForeground(new Color(148, 163, 184));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));

        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtSearch.getText().equals(SEARCH_PLACEHOLDER)) {
                    txtSearch.setText("");
                    txtSearch.setForeground(new Color(30, 41, 59));
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtSearch.getText().trim().isEmpty()) {
                    txtSearch.setForeground(new Color(148, 163, 184));
                    txtSearch.setText(SEARCH_PLACEHOLDER);
                }
            }
        });

        JLabel lblSubj = new JLabel("Subject:");
        lblSubj.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSubj.setForeground(new Color(51, 65, 85));

        cmbSubjectFilter = new JComboBox<>(new String[]{
            "All Subjects", "Advanced Java", "Software Project Management", "Digital Marketing"
        });
        styleModernComboBox(cmbSubjectFilter, 185);

        JLabel lblCat = new JLabel("Category:");
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCat.setForeground(new Color(51, 65, 85));

        cmbCategoryFilter = new JComboBox<>(new String[]{"All", "Exams", "Events", "Urgent", "General"});
        styleModernComboBox(cmbCategoryFilter, 102);

        LoginFrame.ModernButton btnRefresh = new LoginFrame.ModernButton("Refresh", new Color(71, 85, 105), new Color(51, 65, 85));
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setPreferredSize(new Dimension(82, 34));
        btnRefresh.addActionListener(e -> loadDashboardData());

        LoginFrame.ModernButton btnExport = new LoginFrame.ModernButton("Export CSV", new Color(16, 185, 129), new Color(5, 150, 105));
        btnExport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExport.setPreferredSize(new Dimension(98, 34));
        btnExport.addActionListener(e -> exportTableToCSV());

        filterBar.add(lblSearch);
        filterBar.add(txtSearch);
        filterBar.add(lblSubj);
        filterBar.add(cmbSubjectFilter);
        filterBar.add(lblCat);
        filterBar.add(cmbCategoryFilter);
        filterBar.add(btnRefresh);
        filterBar.add(btnExport);

        if (isProfessor) {
            LoginFrame.ModernButton btnQuickEdit = new LoginFrame.ModernButton("Edit / Re-Publish", new Color(37, 99, 235), new Color(29, 78, 216));
            btnQuickEdit.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnQuickEdit.setPreferredSize(new Dimension(126, 34));
            btnQuickEdit.addActionListener(e -> loadSelectedNoticeIntoEditor());
            filterBar.add(btnQuickEdit);
        }

        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.setOpaque(false);
        northWrapper.add(urgentAlertBanner);
        northWrapper.add(Box.createVerticalStrut(6));
        northWrapper.add(statsPanel);
        northWrapper.add(Box.createVerticalStrut(10));
        northWrapper.add(filterBar);
        panel.add(northWrapper, BorderLayout.NORTH);

        // Center Split Area
        JPanel centerSplit = new JPanel(new BorderLayout(16, 0));
        centerSplit.setOpaque(false);

        String[] columns = {"ID", "Notice Title", "Category", "Priority", "Posted By (Subject)", "Expiry Date", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        noticeTable = new JTable(tableModel);
        noticeTable.setRowHeight(42);
        noticeTable.setShowVerticalLines(false);
        noticeTable.setGridColor(new Color(226, 232, 240));
        noticeTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        noticeTable.setSelectionBackground(new Color(239, 246, 255));
        noticeTable.setSelectionForeground(new Color(30, 58, 138));

        JTableHeader header = noticeTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(new Color(71, 85, 105));
        header.setPreferredSize(new Dimension(0, 40));

        noticeTable.getColumnModel().getColumn(6).setMinWidth(0);
        noticeTable.getColumnModel().getColumn(6).setMaxWidth(0);
        noticeTable.getColumnModel().getColumn(0).setMaxWidth(45);
        noticeTable.getColumnModel().getColumn(2).setPreferredWidth(88);
        noticeTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        noticeTable.getColumnModel().getColumn(4).setPreferredWidth(210);

        noticeTable.setDefaultRenderer(Object.class, new PillBadgeTableRenderer());

        noticeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && noticeTable.getSelectedRow() != -1) {
                int modelRow = noticeTable.convertRowIndexToModel(noticeTable.getSelectedRow());
                updatePreviewCard(modelRow);
            }
        });

        rowSorter = new TableRowSorter<>(tableModel);
        noticeTable.setRowSorter(rowSorter);

        Runnable applyFilters = () -> {
            String text = txtSearch.getText().trim();
            if (text.equals(SEARCH_PLACEHOLDER)) text = "";
            String cat = cmbCategoryFilter.getSelectedItem().toString();
            String subj = cmbSubjectFilter.getSelectedItem().toString();

            java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();
            if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            if (!"All".equals(cat)) filters.add(RowFilter.regexFilter("^" + Pattern.quote(cat) + "$", 2));
            if (!"All Subjects".equals(subj)) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(subj), 4));
            rowSorter.setRowFilter(RowFilter.andFilter(filters));
        };

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters.run(); }
        });
        cmbCategoryFilter.addActionListener(e -> applyFilters.run());
        cmbSubjectFilter.addActionListener(e -> applyFilters.run());

        LoginFrame.RoundedPanel tableCard = new LoginFrame.RoundedPanel(16, Color.WHITE);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(noticeTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        styleSlimScrollBar(scrollPane);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerSplit.add(tableCard, BorderLayout.CENTER);
        centerSplit.add(createNoticePreviewCard(), BorderLayout.EAST);

        panel.add(centerSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNoticePreviewCard() {
        LoginFrame.RoundedPanel card = new LoginFrame.RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(350, 0));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);

        JLabel lblSection = new JLabel("NOTICE DETAILS READER");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSection.setForeground(new Color(100, 116, 139));

        lblPreviewBadge = new JLabel("SELECT A NOTICE", SwingConstants.CENTER);
        lblPreviewBadge.setOpaque(true);
        lblPreviewBadge.setBackground(new Color(241, 245, 249));
        lblPreviewBadge.setForeground(new Color(71, 85, 105));
        lblPreviewBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPreviewBadge.setBorder(new EmptyBorder(4, 10, 4, 10));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(lblSection, BorderLayout.WEST);
        headerRow.add(lblPreviewBadge, BorderLayout.EAST);

        lblPreviewTitle = new JLabel("<html>Click any notice row to read full details</html>");
        lblPreviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPreviewTitle.setForeground(new Color(30, 41, 59));

        lblPreviewMeta = new JLabel("Posted By: —  |  Valid Until: —");
        lblPreviewMeta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPreviewMeta.setForeground(new Color(100, 116, 139));

        top.add(headerRow, BorderLayout.NORTH);
        top.add(lblPreviewTitle, BorderLayout.CENTER);
        top.add(lblPreviewMeta, BorderLayout.SOUTH);

        txtPreviewBody = new JTextArea("Select an announcement from the notice board table on the left to inspect its complete instructions, deadlines, and faculty details.");
        txtPreviewBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPreviewBody.setForeground(new Color(51, 65, 85));
        txtPreviewBody.setBackground(new Color(248, 250, 252));
        txtPreviewBody.setLineWrap(true);
        txtPreviewBody.setWrapStyleWord(true);
        txtPreviewBody.setEditable(false);
        txtPreviewBody.setBorder(new EmptyBorder(14, 14, 14, 14));

        JScrollPane bodyScroll = new JScrollPane(txtPreviewBody);
        bodyScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true));
        styleSlimScrollBar(bodyScroll);

        LoginFrame.ModernButton btnPrint = new LoginFrame.ModernButton("Print Notice Slip", new Color(51, 65, 85), new Color(30, 41, 59));
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setPreferredSize(new Dimension(0, 36));
        btnPrint.addActionListener(e -> printSelectedNotice());

        card.add(top, BorderLayout.NORTH);
        card.add(bodyScroll, BorderLayout.CENTER);
        card.add(btnPrint, BorderLayout.SOUTH);
        return card;
    }

    private void printSelectedNotice() {
        int row = noticeTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a notice first to print.", "No Notice Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = noticeTable.convertRowIndexToModel(row);
        String slipText =
            "====================================================\n" +
            "     GURU NANAK INSTITUTE OF MANAGEMENT STUDIES\n" +
            "           DIGITAL NOTICE BOARD - OFFICIAL SLIP\n" +
            "====================================================\n\n" +
            "Notice ID   : #" + tableModel.getValueAt(modelRow, 0) + "\n" +
            "Title       : " + tableModel.getValueAt(modelRow, 1) + "\n" +
            "Category    : " + tableModel.getValueAt(modelRow, 2) + "  |  Priority: " + tableModel.getValueAt(modelRow, 3) + "\n" +
            "Issued By   : " + tableModel.getValueAt(modelRow, 4) + "\n" +
            "Valid Until : " + tableModel.getValueAt(modelRow, 5) + "\n\n" +
            "----------------------------------------------------\n" +
            "DETAILS:\n" +
            tableModel.getValueAt(modelRow, 6) + "\n\n" +
            "----------------------------------------------------\n" +
            "Printed from GNIMS Digital Notice Board Portal";

        JTextArea printArea = new JTextArea(slipText);
        printArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        try {
            printArea.print();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Printer Error: " + ex.getMessage(), "Print Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePreviewCard(int modelRow) {
        String title = tableModel.getValueAt(modelRow, 1).toString();
        String cat = tableModel.getValueAt(modelRow, 2).toString();
        String prio = tableModel.getValueAt(modelRow, 3).toString();
        String postedBy = tableModel.getValueAt(modelRow, 4).toString();
        String expiry = tableModel.getValueAt(modelRow, 5).toString();
        String desc = tableModel.getValueAt(modelRow, 6).toString();

        lblPreviewTitle.setText("<html>" + title + "</html>");
        lblPreviewMeta.setText("<html>By <b>" + postedBy + "</b>  •  Expires " + expiry + "</html>");
        txtPreviewBody.setText(desc);
        txtPreviewBody.setCaretPosition(0);

        lblPreviewBadge.setText(cat.toUpperCase() + " • " + prio.toUpperCase());
        if ("Urgent".equalsIgnoreCase(prio)) {
            lblPreviewBadge.setBackground(new Color(254, 226, 226));
            lblPreviewBadge.setForeground(new Color(185, 28, 28));
        } else {
            lblPreviewBadge.setBackground(new Color(219, 234, 254));
            lblPreviewBadge.setForeground(new Color(29, 78, 216));
        }
    }

    private void styleModernComboBox(JComboBox<String> box, int width) {
        box.setFont(new Font("Segoe UI", Font.BOLD, 12));
        box.setBackground(Color.WHITE);
        box.setForeground(new Color(30, 41, 59));
        box.setPreferredSize(new Dimension(width, 34));
        box.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true));
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▼");
                arrow.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                arrow.setBackground(Color.WHITE);
                arrow.setForeground(new Color(100, 116, 139));
                arrow.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
                arrow.setFocusPainted(false);
                arrow.setContentAreaFilled(false);
                return arrow;
            }
        });
    }

    private void styleSlimScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(7, 0));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = new Color(203, 213, 225);
                this.trackColor = new Color(248, 250, 252);
            }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    static class PillBadgeTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int modelRow = table.convertRowIndexToModel(row);
            String priority = table.getModel().getValueAt(modelRow, 3).toString();
            String text = value != null ? value.toString() : "";

            Color rowBg = isSelected ? new Color(239, 246, 255)
                        : ("Urgent".equalsIgnoreCase(priority) ? new Color(254, 242, 242)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));

            if (column == 2 || column == 3) {
                JPanel cellWrapper = new JPanel(new GridBagLayout());
                cellWrapper.setBackground(rowBg);

                Color pillBg, pillFg;
                if ("Urgent".equalsIgnoreCase(text)) {
                    pillBg = new Color(254, 226, 226); pillFg = new Color(185, 28, 28);
                } else if ("Exams".equalsIgnoreCase(text)) {
                    pillBg = new Color(219, 234, 254); pillFg = new Color(29, 78, 216);
                } else if ("Events".equalsIgnoreCase(text)) {
                    pillBg = new Color(243, 232, 255); pillFg = new Color(107, 33, 168);
                } else if ("Normal".equalsIgnoreCase(text)) {
                    pillBg = new Color(220, 252, 231); pillFg = new Color(21, 128, 61);
                } else {
                    pillBg = new Color(241, 245, 249); pillFg = new Color(71, 85, 105);
                }

                JLabel badge = new JLabel(text) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(pillBg);
                        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
                badge.setForeground(pillFg);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));
                cellWrapper.add(badge);
                return cellWrapper;
            }

            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBorder(new EmptyBorder(0, 12, 0, 12));
            c.setBackground(rowBg);
            if ("Urgent".equalsIgnoreCase(priority) && !isSelected) {
                c.setForeground(new Color(153, 27, 27));
                c.setFont(new Font("Segoe UI", Font.BOLD, 13));
            } else {
                c.setForeground(new Color(30, 41, 59));
                c.setFont(new Font("Segoe UI", column == 1 ? Font.BOLD : Font.PLAIN, 13));
            }
            return c;
        }
    }

    private JPanel createMetricCard(String title, JLabel countLabel, Color accentColor, Color tintBg) {
        LoginFrame.RoundedPanel card = new LoginFrame.RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(8, 6));
        card.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(100, 116, 139));

        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        countLabel.setForeground(accentColor);

        JPanel pill = new JPanel();
        pill.setBackground(tintBg);
        pill.setPreferredSize(new Dimension(6, 38));

        card.add(pill, BorderLayout.WEST);
        JPanel textWrap = new JPanel(new GridLayout(2, 1, 0, 2));
        textWrap.setOpaque(false);
        textWrap.setBorder(new EmptyBorder(0, 10, 0, 0));
        textWrap.add(lblTitle);
        textWrap.add(countLabel);
        card.add(textWrap, BorderLayout.CENTER);

        return card;
    }

    private void loadSelectedNoticeIntoEditor() {
        int row = noticeTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "First select a notice row in the table to edit or re-publish.", "No Notice Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = noticeTable.convertRowIndexToModel(row);
        String noticeOwner = tableModel.getValueAt(modelRow, 4).toString();
        if (!noticeOwner.equals(getFormattedAuthorWithSubject())) {
            JOptionPane.showMessageDialog(this,
                "Access Denied: This notice belongs to " + noticeOwner + ".\nYou can only edit or re-publish your own notices.",
                "Permission Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        editingNoticeId = (Integer) tableModel.getValueAt(modelRow, 0);
        txtAdminTitle.setText(tableModel.getValueAt(modelRow, 1).toString());
        cmbAdminCategory.setSelectedItem(tableModel.getValueAt(modelRow, 2).toString());
        cmbAdminPriority.setSelectedItem(tableModel.getValueAt(modelRow, 3).toString());
        txtAdminExpiry.setText(showingExpiredArchive ? "2026-11-30" : tableModel.getValueAt(modelRow, 5).toString());
        txtAdminDesc.setText(tableModel.getValueAt(modelRow, 6).toString());

        lblAdminFormHeader.setText(showingExpiredArchive ? "Re-Publishing Archived Notice #" + editingNoticeId : "Editing Notice #" + editingNoticeId);
        lblAdminFormSub.setText("Publishing under: " + getFormattedAuthorWithSubject());
        btnAdminSubmit.setText("Save Notice Changes");

        contentCardLayout.show(contentCardsPanel, "ADMIN");
        updateNavPillStyle(btnTabAdmin, true);
        updateNavPillStyle(btnTabFeed, false);
    }

    private void resetAdminForm() {
        editingNoticeId = null;
        txtAdminTitle.setText("");
        txtAdminDesc.setText("");
        cmbAdminCategory.setSelectedIndex(0);
        cmbAdminPriority.setSelectedIndex(0);
        txtAdminExpiry.setText("2026-11-30");
        lblAdminFormHeader.setText("Publish New " + professorSubject + " Notice");
        lblAdminFormSub.setText("Notices published from this account will automatically be tagged with " + getFormattedAuthorWithSubject() + ".");
        btnAdminSubmit.setText("Publish Notice Live");
    }

    private JPanel createProfessorAdminPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(18, 0));
        wrapper.setOpaque(false);

        LoginFrame.RoundedPanel adminCard = new LoginFrame.RoundedPanel(18, Color.WHITE);
        adminCard.setLayout(new BorderLayout(0, 18));
        adminCard.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        lblAdminFormHeader = new JLabel("Publish New " + professorSubject + " Notice");
        lblAdminFormHeader.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblAdminFormHeader.setForeground(new Color(30, 41, 59));
        lblAdminFormSub = new JLabel("Notices published from this account will automatically be tagged with " + getFormattedAuthorWithSubject() + ".");
        lblAdminFormSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblAdminFormSub.setForeground(new Color(100, 116, 139));
        titleBox.add(lblAdminFormHeader);
        titleBox.add(lblAdminFormSub);
        adminCard.add(titleBox, BorderLayout.NORTH);

        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        txtAdminTitle = new JTextField();
        styleFormTextField(txtAdminTitle);

        JPanel row3Col = new JPanel(new GridLayout(1, 3, 16, 0));
        row3Col.setOpaque(false);
        row3Col.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        row3Col.setAlignmentX(Component.LEFT_ALIGNMENT);

        cmbAdminCategory = new JComboBox<>(new String[]{"General", "Exams", "Events", "Urgent"});
        styleModernComboBox(cmbAdminCategory, 0);
        cmbAdminCategory.setPreferredSize(new Dimension(0, 38));

        cmbAdminPriority = new JComboBox<>(new String[]{"Normal", "Urgent"});
        styleModernComboBox(cmbAdminPriority, 0);
        cmbAdminPriority.setPreferredSize(new Dimension(0, 38));

        txtAdminExpiry = new JTextField("2026-11-30");
        styleFormTextField(txtAdminExpiry);

        row3Col.add(wrapFieldWithLabel("Category", cmbAdminCategory));
        row3Col.add(wrapFieldWithLabel("Priority Level", cmbAdminPriority));
        row3Col.add(wrapFieldWithLabel("Expiry Date (YYYY-MM-DD)", txtAdminExpiry));

        txtAdminDesc = new JTextArea(6, 30);
        txtAdminDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtAdminDesc.setForeground(new Color(30, 41, 59));
        txtAdminDesc.setLineWrap(true);
        txtAdminDesc.setWrapStyleWord(true);
        txtAdminDesc.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane descScroll = new JScrollPane(txtAdminDesc);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true));
        styleSlimScrollBar(descScroll);

        formBody.add(createAdminLabel("Notice Title"));
        formBody.add(Box.createVerticalStrut(6));
        formBody.add(txtAdminTitle);
        formBody.add(Box.createVerticalStrut(16));
        formBody.add(row3Col);
        formBody.add(Box.createVerticalStrut(16));
        formBody.add(createAdminLabel("Detailed Notice Description"));
        formBody.add(Box.createVerticalStrut(6));
        formBody.add(descScroll);

        adminCard.add(formBody, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnPanel.setOpaque(false);

        btnAdminSubmit = new LoginFrame.ModernButton("Publish Notice Live", new Color(37, 99, 235), new Color(29, 78, 216));
        btnAdminSubmit.setPreferredSize(new Dimension(195, 40));

        LoginFrame.ModernButton btnClear = new LoginFrame.ModernButton("Reset / New Mode", new Color(100, 116, 139), new Color(71, 85, 105));
        btnClear.setPreferredSize(new Dimension(150, 40));

        btnAdminSubmit.addActionListener(e -> {
            String title = txtAdminTitle.getText().trim();
            String desc = txtAdminDesc.getText().trim();
            String cat = cmbAdminCategory.getSelectedItem().toString();
            String prio = cmbAdminPriority.getSelectedItem().toString();
            String exp = txtAdminExpiry.getText().trim();

            if (title.isEmpty() || desc.isEmpty() || exp.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in Title, Expiry Date, and Description.", "Required Fields", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success;
            if (editingNoticeId == null) {
                success = noticeDAO.addNotice(title, desc, cat, prio, getFormattedAuthorWithSubject(), exp);
            } else {
                success = noticeDAO.updateNotice(editingNoticeId, title, desc, cat, prio, exp, getFormattedAuthorWithSubject());
            }

            if (success) {
                JOptionPane.showMessageDialog(this, editingNoticeId == null ? "Notice Published Live under " + professorSubject + "!" : "Notice #" + editingNoticeId + " Updated & Live!");
                resetAdminForm();
                showingExpiredArchive = false;
                btnToggleArchive.setText("View Expired Archive");
                btnTabFeed.setText("Active Notice Board");
                loadDashboardData();
                contentCardLayout.show(contentCardsPanel, "FEED");
                updateNavPillStyle(btnTabFeed, true);
                if (btnTabAdmin != null) updateNavPillStyle(btnTabAdmin, false);
            } else {
                JOptionPane.showMessageDialog(this, "Database Error or Permission Denied. Check date format (YYYY-MM-DD).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnClear.addActionListener(e -> resetAdminForm());

        btnPanel.add(btnAdminSubmit);
        btnPanel.add(btnClear);
        adminCard.add(btnPanel, BorderLayout.SOUTH);

        // Right: Archive & Manage Sidebar
        LoginFrame.RoundedPanel sideCard = new LoginFrame.RoundedPanel(18, Color.WHITE);
        sideCard.setLayout(new BorderLayout(0, 14));
        sideCard.setPreferredSize(new Dimension(330, 0));
        sideCard.setBorder(new EmptyBorder(24, 22, 24, 22));

        JLabel lblManageTitle = new JLabel("Archive & Quick Actions");
        lblManageTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblManageTitle.setForeground(new Color(30, 41, 59));

        JTextArea txtTips = new JTextArea(
            "• Ownership Protection:\n  You can view all campus notices, but you can only edit or delete notices published by you.\n\n" +
            "• Expired Notices Archive:\n  Inspect past expired notices and re-publish your own notices with a new deadline.\n\n" +
            "• Keyboard Shortcuts:\n  Press Ctrl+F on the notice board to jump straight to Search."
        );
        txtTips.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTips.setForeground(new Color(71, 85, 105));
        txtTips.setBackground(new Color(248, 250, 252));
        txtTips.setLineWrap(true);
        txtTips.setWrapStyleWord(true);
        txtTips.setEditable(false);
        txtTips.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel sideBtns = new JPanel(new GridLayout(3, 1, 0, 10));
        sideBtns.setOpaque(false);

        btnToggleArchive = new LoginFrame.ModernButton("View Expired Archive", new Color(217, 119, 6), new Color(180, 83, 9));
        btnToggleArchive.setPreferredSize(new Dimension(0, 38));
        btnToggleArchive.addActionListener(e -> {
            showingExpiredArchive = !showingExpiredArchive;
            btnToggleArchive.setText(showingExpiredArchive ? "Back to Active Notices" : "View Expired Archive");
            btnTabFeed.setText(showingExpiredArchive ? "Expired Notices Archive" : "Active Notice Board");
            loadDashboardData();
            contentCardLayout.show(contentCardsPanel, "FEED");
            updateNavPillStyle(btnTabFeed, true);
            updateNavPillStyle(btnTabAdmin, false);
        });

        LoginFrame.ModernButton btnLoadEdit = new LoginFrame.ModernButton("Load Selected Row to Edit", new Color(37, 99, 235), new Color(29, 78, 216));
        btnLoadEdit.setPreferredSize(new Dimension(0, 38));
        btnLoadEdit.addActionListener(e -> loadSelectedNoticeIntoEditor());

        LoginFrame.ModernButton btnDelete = new LoginFrame.ModernButton("Delete Selected Notice", new Color(220, 38, 38), new Color(185, 28, 28));
        btnDelete.setPreferredSize(new Dimension(0, 38));
        btnDelete.addActionListener(e -> {
            int row = noticeTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "First select a row in the notice table to delete.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = noticeTable.convertRowIndexToModel(row);
            int id = (int) tableModel.getValueAt(modelRow, 0);
            String noticeOwner = tableModel.getValueAt(modelRow, 4).toString();

            if (!noticeOwner.equals(getFormattedAuthorWithSubject())) {
                JOptionPane.showMessageDialog(this,
                    "Access Denied: This notice was published by " + noticeOwner + ".\nYou can only delete your own notices.",
                    "Permission Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (JOptionPane.showConfirmDialog(this, "Permanently delete your Notice #" + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (noticeDAO.deleteNotice(id, getFormattedAuthorWithSubject())) {
                    resetAdminForm();
                    loadDashboardData();
                    JOptionPane.showMessageDialog(this, "Your notice was deleted.");
                }
            }
        });

        sideBtns.add(btnToggleArchive);
        sideBtns.add(btnLoadEdit);
        sideBtns.add(btnDelete);

        sideCard.add(lblManageTitle, BorderLayout.NORTH);
        sideCard.add(txtTips, BorderLayout.CENTER);
        sideCard.add(sideBtns, BorderLayout.SOUTH);

        wrapper.add(adminCard, BorderLayout.CENTER);
        wrapper.add(sideCard, BorderLayout.EAST);
        return wrapper;
    }

    private JLabel createAdminLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void styleFormTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(new Color(30, 41, 59));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setPreferredSize(new Dimension(0, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));
    }

    private JPanel wrapFieldWithLabel(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(createAdminLabel(labelText), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void loadDashboardData() {
        tableModel.setRowCount(0);
        List<Object[]> notices = showingExpiredArchive ? noticeDAO.getExpiredNotices() : noticeDAO.getActiveNotices();
        for (Object[] row : notices) {
            tableModel.addRow(row);
        }

        int[] stats = noticeDAO.getDashboardStats();
        lblTotalCount.setText(String.valueOf(stats[0]));
        lblUrgentCount.setText(String.valueOf(stats[1]));
        lblExpiringCount.setText(String.valueOf(stats[2]));

        if (lastKnownUrgentCount != -1 && stats[1] > lastKnownUrgentCount) {
            Toolkit.getDefaultToolkit().beep();
            lblUrgentBannerText.setText("NEW URGENT ALERT DETECTED — Urgent notices increased from " + lastKnownUrgentCount + " to " + stats[1] + "!");
            urgentAlertBanner.setVisible(true);
        }
        lastKnownUrgentCount = stats[1];

        if (tableModel.getRowCount() > 0 && noticeTable.getSelectedRow() == -1) {
            noticeTable.setRowSelectionInterval(0, 0);
        }
    }

    private void exportTableToCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("GNIMS_Digital_Notice_Board_Export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fc.getSelectedFile()))) {
                pw.println("ID,Title,Category,Priority,Posted By (Subject),Expiry Date,Description");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    pw.printf("%s,\"%s\",%s,%s,\"%s\",%s,\"%s\"%n",
                        tableModel.getValueAt(i, 0),
                        tableModel.getValueAt(i, 1).toString().replace("\"", "\"\""),
                        tableModel.getValueAt(i, 2),
                        tableModel.getValueAt(i, 3),
                        tableModel.getValueAt(i, 4),
                        tableModel.getValueAt(i, 5),
                        tableModel.getValueAt(i, 6).toString().replace("\"", "\"\"")
                    );
                }
                JOptionPane.showMessageDialog(this, "CSV Exported Successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export Error: " + ex.getMessage());
            }
        }
    }
}