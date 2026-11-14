import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123"; // Demo only; change before production.

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/online_exam_db";
    private static final String DB_USER = "root";

    // Set MYSQL_PASSWORD as an environment variable before running.
    // This avoids publishing your actual database password on GitHub.
    private static final String DB_PASSWORD =
            System.getenv().getOrDefault("MYSQL_PASSWORD", "");

    private final Map<String, String> studentPasswords = new HashMap<>();
    private String loggedInStudentId;

    private final String[] columns = {
            "ID", "Student Name", "Course", "Exam Name",
            "Marks", "Total Marks", "Percentage", "Result"
    };

    public Main() {
        setTitle("Online Examination and Result Management System");
        setSize(1100, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        loadData();
        showLoginPortal();
    }

    // ================= DATABASE CONNECTION =================

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // ================= LOAD DATA FROM MYSQL =================

    private void loadData() {
        String sql = "SELECT * FROM exam_records ORDER BY student_id";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            model.setRowCount(0);
            studentPasswords.clear();

            while (rs.next()) {
                String id = rs.getString("student_id");

                model.addRow(new Object[]{
                        id,
                        rs.getString("student_name"),
                        rs.getString("course"),
                        rs.getString("exam_name"),
                        rs.getInt("marks"),
                        rs.getInt("total_marks"),
                        rs.getString("percentage"),
                        rs.getString("result")
                });

                studentPasswords.put(id, rs.getString("student_password"));
            }

        } catch (SQLException e) {
            showMessage("Could not load database data:\n" + e.getMessage());
        }
    }

    // ================= LOGIN PORTAL =================

    private void showLoginPortal() {
        getContentPane().removeAll();
        setLayout(new BorderLayout());

        JLabel heading = new JLabel(
                "ONLINE EXAMINATION AND RESULT MANAGEMENT SYSTEM",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setBorder(BorderFactory.createEmptyBorder(30, 10, 30, 10));
        add(heading, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(3, 1, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(70, 250, 70, 250));

        JButton adminButton = new JButton("Admin Portal");
        JButton studentButton = new JButton("Student Portal");
        JButton exitButton = new JButton("Exit");

        panel.add(adminButton);
        panel.add(studentButton);
        panel.add(exitButton);
        add(panel, BorderLayout.CENTER);

        adminButton.addActionListener(e -> adminLogin());
        studentButton.addActionListener(e -> studentLogin());
        exitButton.addActionListener(e -> System.exit(0));

        refreshWindow();
    }

    // ================= ADMIN LOGIN =================

    private void adminLogin() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();

        Object[] fields = {
                "Admin Username:", username,
                "Admin Password:", password
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Admin Login", JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        if (username.getText().trim().equals(ADMIN_USERNAME)
                && new String(password.getPassword()).equals(ADMIN_PASSWORD)) {
            showAdminDashboard();
        } else {
            showMessage("Invalid admin username or password.");
        }
    }

    // ================= STUDENT LOGIN =================

    private void studentLogin() {
        JTextField idField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        Object[] fields = {
                "Student ID:", idField,
                "Student Password:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Student Login", JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String id = idField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (findRow(id) != -1
                && studentPasswords.containsKey(id)
                && studentPasswords.get(id).equals(password)) {
            loggedInStudentId = id;
            showStudentDashboard();
        } else {
            showMessage("Invalid Student ID or password.");
        }
    }

    // ================= ADMIN DASHBOARD =================

    private void showAdminDashboard() {
        getContentPane().removeAll();
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "ADMIN DASHBOARD - STUDENT AND RESULT MANAGEMENT",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        heading.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));
        add(heading, BorderLayout.NORTH);

        table = new JTable(model);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout());

        JButton addButton = new JButton("Add Student");
        JButton updateButton = new JButton("Update Student");
        JButton deleteButton = new JButton("Delete Student");
        JButton searchButton = new JButton("Search by ID");
        JButton showButton = new JButton("Show All");
        JButton logoutButton = new JButton("Logout");

        searchField = new JTextField(8);

        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(new JLabel("ID:"));
        buttons.add(searchField);
        buttons.add(searchButton);
        buttons.add(showButton);
        buttons.add(logoutButton);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addRecord());
        updateButton.addActionListener(e -> updateRecord());
        deleteButton.addActionListener(e -> deleteRecord());
        searchButton.addActionListener(e -> searchRecord());
        showButton.addActionListener(e -> showAllRecords());

        logoutButton.addActionListener(e -> {
            loggedInStudentId = null;
            showLoginPortal();
        });

        refreshWindow();
    }

    // ================= STUDENT DASHBOARD =================

    private void showStudentDashboard() {
        getContentPane().removeAll();
        setLayout(new BorderLayout(10, 10));

        int row = findRow(loggedInStudentId);

        if (row == -1) {
            showMessage("Student record not found.");
            loggedInStudentId = null;
            showLoginPortal();
            return;
        }

        JLabel heading = new JLabel(
                "STUDENT PORTAL - Welcome, " + model.getValueAt(row, 1),
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        heading.setBorder(BorderFactory.createEmptyBorder(25, 5, 25, 5));
        add(heading, BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridLayout(4, 1, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(70, 250, 70, 250));

        JButton profileButton = new JButton("View My Profile");
        JButton examButton = new JButton("Take Online Examination");
        JButton resultButton = new JButton("View My Result");
        JButton logoutButton = new JButton("Logout");

        panel.add(profileButton);
        panel.add(examButton);
        panel.add(resultButton);
        panel.add(logoutButton);
        add(panel, BorderLayout.CENTER);

        profileButton.addActionListener(e -> viewMyProfile());
        examButton.addActionListener(e -> takeOnlineExam());
        resultButton.addActionListener(e -> viewMyResult());

        logoutButton.addActionListener(e -> {
            loggedInStudentId = null;
            showLoginPortal();
        });

        refreshWindow();
    }

    // ================= CREATE STUDENT =================

    private void addRecord() {
        String idInput = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (idInput == null) return;

        String id = idInput.trim();
        if (id.isEmpty() || findRow(id) != -1) {
            showMessage("Student ID is empty or already exists.");
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Enter Student Name:");
        if (name == null) return;

        String course = JOptionPane.showInputDialog(this, "Enter Course:");
        if (course == null) return;

        JPasswordField passwordField = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(
                this, passwordField, "Set Student Login Password",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (option != JOptionPane.OK_OPTION) return;

        String password = new String(passwordField.getPassword());
        name = name.trim();
        course = course.trim();

        if (name.isEmpty() || course.isEmpty() || password.trim().isEmpty()) {
            showMessage("Name, course and password cannot be empty.");
            return;
        }

        String sql = "INSERT INTO exam_records "
                + "(student_id, student_name, course, exam_name, "
                + "marks, total_marks, percentage, result, student_password) "
                + "VALUES (?, ?, ?, 'Not Attempted', 0, 0, 'N/A', "
                + "'Not Attempted', ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, course);
            ps.setString(4, password);
            ps.executeUpdate();

            studentPasswords.put(id, password);
            model.addRow(new Object[]{
                    id, name, course, "Not Attempted",
                    0, 0, "N/A", "Not Attempted"
            });

            showMessage("Student added successfully to MySQL.");

        } catch (SQLException e) {
            showMessage("Could not add student:\n" + e.getMessage());
        }
    }

    // ================= READ STUDENTS =================

    private void showAllRecords() {
        loadData();
        table.clearSelection();
        searchField.setText("");
    }

    // ================= UPDATE STUDENT =================

    private void updateRecord() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showMessage("Select a student record first.");
            return;
        }

        String id = model.getValueAt(row, 0).toString();

        String name = JOptionPane.showInputDialog(
                this, "Student Name:", model.getValueAt(row, 1)
        );
        if (name == null) return;

        String course = JOptionPane.showInputDialog(
                this, "Course:", model.getValueAt(row, 2)
        );
        if (course == null) return;

        int changePassword = JOptionPane.showConfirmDialog(
                this, "Do you want to change this student's password?",
                "Password", JOptionPane.YES_NO_OPTION
        );

        String newPassword = studentPasswords.get(id);

        if (changePassword == JOptionPane.YES_OPTION) {
            JPasswordField field = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(
                    this, field, "Enter New Password",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (option != JOptionPane.OK_OPTION) return;

            newPassword = new String(field.getPassword());
            if (newPassword.trim().isEmpty()) {
                showMessage("Password cannot be empty.");
                return;
            }
        }

        name = name.trim();
        course = course.trim();

        if (name.isEmpty() || course.isEmpty()) {
            showMessage("Name and course cannot be empty.");
            return;
        }

        String sql = "UPDATE exam_records SET student_name=?, "
                + "course=?, student_password=? WHERE student_id=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, course);
            ps.setString(3, newPassword);
            ps.setString(4, id);
            ps.executeUpdate();

            studentPasswords.put(id, newPassword);
            model.setValueAt(name, row, 1);
            model.setValueAt(course, row, 2);
            showMessage("Student record updated in MySQL.");

        } catch (SQLException e) {
            showMessage("Could not update student:\n" + e.getMessage());
        }
    }

    // ================= DELETE STUDENT =================

    private void deleteRecord() {
        int row = table.getSelectedRow();
        if (row == -1) {
            showMessage("Select a student to delete.");
            return;
        }

        String id = model.getValueAt(row, 0).toString();

        int choice = JOptionPane.showConfirmDialog(
                this, "Delete this student and their result?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM exam_records WHERE student_id=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, id);
            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                model.removeRow(row);
                studentPasswords.remove(id);
                showMessage("Student deleted from MySQL.");
            } else {
                showMessage("Student record was not found in MySQL.");
                loadData();
            }

        } catch (SQLException e) {
            showMessage("Could not delete student:\n" + e.getMessage());
        }
    }

    // ================= SEARCH STUDENT =================

    private void searchRecord() {
        String id = searchField.getText().trim();
        if (id.isEmpty()) {
            showMessage("Enter a Student ID.");
            return;
        }

        int row = findRow(id);
        if (row != -1) {
            table.setRowSelectionInterval(row, row);
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        } else {
            showMessage("Student not found.");
        }
    }

    private int findRow(String id) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).toString().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }

    // ================= STUDENT PROFILE =================

    private void viewMyProfile() {
        int row = findRow(loggedInStudentId);
        if (row == -1) return;

        JOptionPane.showMessageDialog(
                this,
                "MY PROFILE\n\n"
                        + "Student ID: " + model.getValueAt(row, 0) + "\n"
                        + "Name: " + model.getValueAt(row, 1) + "\n"
                        + "Course: " + model.getValueAt(row, 2),
                "Student Profile",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ================= ONLINE EXAMINATION =================

    private void takeOnlineExam() {
        int row = findRow(loggedInStudentId);
        if (row == -1) {
            showMessage("Student record not found.");
            return;
        }

        if (!"Not Attempted".equals(model.getValueAt(row, 7).toString())) {
            showMessage("You have already attempted this exam.");
            return;
        }

        String[] questions = {
                "1. Which language is used for Java applications?",
                "2. Which keyword creates an object in Java?",
                "3. Which method is the entry point of Java?",
                "4. Which data type stores true or false?",
                "5. Which SQL command retrieves records?"
        };

        String[][] options = {
                {"HTML", "Java", "CSS", "SQL"},
                {"class", "new", "static", "void"},
                {"start()", "run()", "main()", "init()"},
                {"int", "String", "boolean", "float"},
                {"INSERT", "UPDATE", "SELECT", "DELETE"}
        };

        int[] answers = {1, 1, 2, 2, 2};
        int score = 0;

        for (int i = 0; i < questions.length; i++) {
            Object selected = JOptionPane.showInputDialog(
                    this, questions[i],
                    "Online Exam - Question " + (i + 1),
                    JOptionPane.QUESTION_MESSAGE, null,
                    options[i], options[i][0]
            );

            if (selected == null) {
                showMessage("Examination cancelled.");
                return;
            }

            for (int j = 0; j < options[i].length; j++) {
                if (selected.equals(options[i][j]) && j == answers[i]) {
                    score++;
                }
            }
        }

        int total = questions.length;
        double percentage = score * 100.0 / total;
        String percentageText = String.format("%.2f%%", percentage);
        String result = percentage >= 40 ? "PASS" : "FAIL";

        String sql = "UPDATE exam_records SET exam_name=?, marks=?, "
                + "total_marks=?, percentage=?, result=? WHERE student_id=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "Java Online Exam");
            ps.setInt(2, score);
            ps.setInt(3, total);
            ps.setString(4, percentageText);
            ps.setString(5, result);
            ps.setString(6, loggedInStudentId);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                showMessage("Could not save exam result.");
                return;
            }

            model.setValueAt("Java Online Exam", row, 3);
            model.setValueAt(score, row, 4);
            model.setValueAt(total, row, 5);
            model.setValueAt(percentageText, row, 6);
            model.setValueAt(result, row, 7);

            JOptionPane.showMessageDialog(
                    this,
                    "EXAM COMPLETED\n\n"
                            + "Score: " + score + "/" + total + "\n"
                            + "Percentage: " + percentageText + "\n"
                            + "Result: " + result,
                    "Examination Result",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException e) {
            showMessage("Could not save examination result:\n" + e.getMessage());
        }
    }

    // ================= STUDENT RESULT =================

    private void viewMyResult() {
        int row = findRow(loggedInStudentId);
        if (row == -1) return;

        JOptionPane.showMessageDialog(
                this,
                "MY EXAMINATION RESULT\n\n"
                        + "Student: " + model.getValueAt(row, 1) + "\n"
                        + "Exam: " + model.getValueAt(row, 3) + "\n"
                        + "Marks: " + model.getValueAt(row, 4) + "/"
                        + model.getValueAt(row, 5) + "\n"
                        + "Percentage: " + model.getValueAt(row, 6) + "\n"
                        + "Result: " + model.getValueAt(row, 7),
                "My Result",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ================= UTILITY METHODS =================

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void refreshWindow() {
        revalidate();
        repaint();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ================= MAIN METHOD =================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
