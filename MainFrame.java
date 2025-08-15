import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MainFrame extends JFrame {
    private String username, role;
    private Connection conn;

    private JTabbedPane tabbedPane;
    private JTable roomsTable, bookingsTable;
    private JTextField roomIdBookField, checkInField, checkOutField;
    private JLabel bookingMsg;

    public MainFrame(String username, String role) {
        this.username = username;
        this.role = role;

        setTitle("Hotel Booking System - " + role + ": " + username);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        conn = DBConnection.getConnection();
        tabbedPane = new JTabbedPane();

        if (role.equals("Admin")) {
            tabbedPane.addTab("Add Room", getAddRoomPanel());
            tabbedPane.addTab("View Bookings", getViewAllBookingsPanel());
        }

        tabbedPane.addTab("View Rooms", getViewRoomsPanel());
        tabbedPane.addTab("Book Room", getBookRoomPanel());
        tabbedPane.addTab("My Bookings", getMyBookingsPanel());

        add(tabbedPane);
        setVisible(true);
    }

    private JPanel getAddRoomPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JTextField roomNumberField = new JTextField(10);
        JTextField roomTypeField = new JTextField(10);
        JTextField priceField = new JTextField(10);
        JButton addRoomBtn = new JButton("Add Room");
        JLabel msgLabel = new JLabel("");

        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx=0; gbc.gridy=0; panel.add(new JLabel("Room Number:"), gbc);
        gbc.gridx=1; panel.add(roomNumberField, gbc);
        gbc.gridx=0; gbc.gridy=1; panel.add(new JLabel("Room Type:"), gbc);
        gbc.gridx=1; panel.add(roomTypeField, gbc);
        gbc.gridx=0; gbc.gridy=2; panel.add(new JLabel("Price:"), gbc);
        gbc.gridx=1; panel.add(priceField, gbc);
        gbc.gridx=0; gbc.gridy=3; panel.add(addRoomBtn, gbc);
        gbc.gridx=1; panel.add(msgLabel, gbc);

        addRoomBtn.addActionListener(e -> {
            try {
                String num = roomNumberField.getText();
                String type = roomTypeField.getText();
                double price = Double.parseDouble(priceField.getText());

                PreparedStatement ps = conn.prepareStatement("INSERT INTO rooms(room_number, room_type, price, status) VALUES (?, ?, ?, 'Available')");
                ps.setString(1, num);
                ps.setString(2, type);
                ps.setDouble(3, price);
                ps.executeUpdate();

                msgLabel.setText("Room added!");
                roomNumberField.setText("");
                roomTypeField.setText("");
                priceField.setText("");
            } catch (Exception ex) {
                msgLabel.setText("Error: "+ex.getMessage());
            }
        });

        return panel;
    }

    private JPanel getViewRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadAvailableRooms());

        roomsTable = new JTable();
        panel.add(refreshBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(roomsTable), BorderLayout.CENTER);

        loadAvailableRooms();
        return panel;
    }

    private void loadAvailableRooms() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT room_id, room_number, room_type, price, status FROM rooms WHERE status='Available'");

            DefaultTableModel model = new DefaultTableModel(new String[]{"Room ID","Room Number","Type","Price","Status"},0);
            while(rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("room_id"),
                    rs.getString("room_number"),
                    rs.getString("room_type"),
                    rs.getDouble("price"),
                    rs.getString("status")
                });
            }
            roomsTable.setModel(model);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,"Error loading rooms:\n"+ex.getMessage());
        }
    }

    private JPanel getBookRoomPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel roomIdLabel = new JLabel("Room ID:");
        JLabel checkInLabel = new JLabel("Check-in (YYYY-MM-DD):");
        JLabel checkOutLabel = new JLabel("Check-out (YYYY-MM-DD):");

        roomIdBookField = new JTextField(10);
        checkInField = new JTextField(10);
        checkOutField = new JTextField(10);
        bookingMsg = new JLabel();

        JButton bookBtn = new JButton("Book Room");

        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx=0; gbc.gridy=0; panel.add(roomIdLabel, gbc);
        gbc.gridx=1; panel.add(roomIdBookField, gbc);
        gbc.gridx=0; gbc.gridy=1; panel.add(checkInLabel, gbc);
        gbc.gridx=1; panel.add(checkInField, gbc);
        gbc.gridx=0; gbc.gridy=2; panel.add(checkOutLabel, gbc);
        gbc.gridx=1; panel.add(checkOutField, gbc);
        gbc.gridx=0; gbc.gridy=3; panel.add(bookBtn, gbc);
        gbc.gridx=1; panel.add(bookingMsg, gbc);

        bookBtn.addActionListener(e -> bookRoomAction());

        return panel;
    }

    private void bookRoomAction() {
        try {
            int roomId = Integer.parseInt(roomIdBookField.getText());
            String checkIn = checkInField.getText();
            String checkOut = checkOutField.getText();

            PreparedStatement psUser = conn.prepareStatement("SELECT user_id FROM users WHERE username=?");
            psUser.setString(1, username);
            ResultSet rsUser = psUser.executeQuery();
            if (!rsUser.next()) {
                bookingMsg.setText("User not found");
                return;
            }
            int customerId = rsUser.getInt("user_id");

            PreparedStatement psBooking = conn.prepareStatement(
                "INSERT INTO bookings(customer_id, room_id, check_in, check_out) VALUES (?, ?, ?, ?)"
            );
            psBooking.setInt(1, customerId);
            psBooking.setInt(2, roomId);
            psBooking.setString(3, checkIn);
            psBooking.setString(4, checkOut);
            psBooking.executeUpdate();

            PreparedStatement psRoomUpdate = conn.prepareStatement("UPDATE rooms SET status='Booked' WHERE room_id=?");
            psRoomUpdate.setInt(1, roomId);
            psRoomUpdate.executeUpdate();

            bookingMsg.setText("Room booked successfully!");
            roomIdBookField.setText("");
            checkInField.setText("");
            checkOutField.setText("");
        } catch (Exception ex) {
            bookingMsg.setText("Booking failed: " + ex.getMessage());
        }
    }

    private JPanel getViewAllBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadAllBookings());

        bookingsTable = new JTable();
        panel.add(refreshBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(bookingsTable), BorderLayout.CENTER);

        loadAllBookings();
        return panel;
    }

    private void loadAllBookings() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT b.booking_id, u.username, r.room_number, b.check_in, b.check_out " +
                "FROM bookings b JOIN users u ON b.customer_id = u.user_id " +
                "JOIN rooms r ON b.room_id = r.room_id"
            );

            DefaultTableModel model = new DefaultTableModel(new String[]{"Booking ID", "Customer", "Room Number", "Check-in", "Check-out"},0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("booking_id"),
                    rs.getString("username"),
                    rs.getString("room_number"),
                    rs.getString("check_in"),
                    rs.getString("check_out")
                });
            }
            bookingsTable.setModel(model);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading bookings:\n" + ex.getMessage());
        }
    }

    private JPanel getMyBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadMyBookings());

        bookingsTable = new JTable();
        JButton cancelBtn = new JButton("Cancel Selected Booking");

        cancelBtn.addActionListener(e -> cancelSelectedBooking());

        panel.add(refreshBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(bookingsTable), BorderLayout.CENTER);
        panel.add(cancelBtn, BorderLayout.SOUTH);

        loadMyBookings();
        return panel;
    }

    private void loadMyBookings() {
        try {
            PreparedStatement psUser = conn.prepareStatement("SELECT user_id FROM users WHERE username=?");
            psUser.setString(1, username);
            ResultSet rsUser = psUser.executeQuery();
            if (!rsUser.next()) return;
            int customerId = rsUser.getInt("user_id");

            PreparedStatement ps = conn.prepareStatement(
                "SELECT b.booking_id, r.room_number, b.check_in, b.check_out " +
                "FROM bookings b JOIN rooms r ON b.room_id = r.room_id WHERE b.customer_id=?"
            );
            ps.setInt(1, customerId);

            ResultSet rs = ps.executeQuery();

            DefaultTableModel model = new DefaultTableModel(new String[]{"Booking ID", "Room Number", "Check-in", "Check-out"}, 0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("booking_id"),
                    rs.getString("room_number"),
                    rs.getString("check_in"),
                    rs.getString("check_out")
                });
            }
            bookingsTable.setModel(model);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading your bookings:\n" + ex.getMessage());
        }
    }

    private void cancelSelectedBooking() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "No booking selected.");
            return;
        }
        int bookingId = (Integer) bookingsTable.getValueAt(selectedRow, 0);

        try {
            PreparedStatement psRoomId = conn.prepareStatement("SELECT room_id FROM bookings WHERE booking_id = ?");
            psRoomId.setInt(1, bookingId);
            ResultSet rsRoomId = psRoomId.executeQuery();
            if (!rsRoomId.next()) return;

            int roomId = rsRoomId.getInt("room_id");

            PreparedStatement psDelete = conn.prepareStatement("DELETE FROM bookings WHERE booking_id = ?");
            psDelete.setInt(1, bookingId);
            psDelete.executeUpdate();

            PreparedStatement psUpdateRoom = conn.prepareStatement("UPDATE rooms SET status='Available' WHERE room_id = ?");
            psUpdateRoom.setInt(1, roomId);
            psUpdateRoom.executeUpdate();

            JOptionPane.showMessageDialog(this, "Booking cancelled successfully.");
            loadMyBookings();
            loadAvailableRooms();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cancelling booking: " + ex.getMessage());
        }
    }
}
