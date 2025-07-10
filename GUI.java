import app.AppException;
import app.checkin.CheckInRoomForm;
import app.checkout.CheckOutRoomForm;
import app.reservation.ReserveRoomForm;
import domain.payment.Payment;
import domain.reservation.Reservation;
import domain.room.Room;
import util.DateUtil;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// ① メインのGUIクラス
public class GUI extends JFrame {

    private final GuiDbConnector dbConnector = new GuiDbConnector();
    private JPanel reservedPanel, checkedInPanel, checkedOutPanel;
    private JLabel statusBar;
    private JRadioButton sortDescRadio, sortAscRadio;
    private JLabel reservedCountLabel, checkedInCountLabel, checkedOutCountLabel;
    private JTextField dateSearchField; // ★★★ 日付検索用のテキストフィールド

    public GUI() {
        setTitle("ホテル予約システム GUI");
        setSize(1200, 800); // 横幅を広げる
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- 上部パネル (ソート、検索、ダッシュボード) ---
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // ソート機能部分
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sortDescRadio = new JRadioButton("日付が新しい順", true);
        sortAscRadio = new JRadioButton("日付が古い順");
        ButtonGroup sortGroup = new ButtonGroup();
        sortGroup.add(sortDescRadio);
        sortGroup.add(sortAscRadio);
        sortPanel.add(new JLabel("並び順:"));
        sortPanel.add(sortDescRadio);
        sortPanel.add(sortAscRadio);
        
        // ★★★ 日付検索部分
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateSearchField = new JTextField(10);
        JButton clearSearchButton = new JButton("クリア");
        searchPanel.add(new JLabel("日付で検索 (yyyy/MM/dd):"));
        searchPanel.add(dateSearchField);
        searchPanel.add(clearSearchButton);

        // ダッシュボード部分
        JPanel dashboardPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Font dashboardFont = new Font("SansSerif", Font.BOLD, 14);
        reservedCountLabel = new JLabel("予約中: -");
        checkedInCountLabel = new JLabel("チェックイン済: -");
        checkedOutCountLabel = new JLabel("チェックアウト済: -");
        reservedCountLabel.setFont(dashboardFont);
        checkedInCountLabel.setFont(dashboardFont);
        checkedOutCountLabel.setFont(dashboardFont);
        dashboardPanel.add(reservedCountLabel);
        dashboardPanel.add(new JSeparator(SwingConstants.VERTICAL));
        dashboardPanel.add(checkedInCountLabel);
        dashboardPanel.add(new JSeparator(SwingConstants.VERTICAL));
        dashboardPanel.add(checkedOutCountLabel);
        
        // 上部パネルに各パーツを配置
        topPanel.add(sortPanel, BorderLayout.WEST);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(dashboardPanel, BorderLayout.EAST);

        // --- 中央パネル（3つのグループを縦に並べる） ---
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));

        reservedPanel = createGroupPanel("予約中 (Reserved)");
        checkedInPanel = createGroupPanel("チェックイン済み (Checked-in)");
        checkedOutPanel = createGroupPanel("チェックアウト済み (Checked-out)");

        mainContentPanel.add(new JScrollPane(reservedPanel));
        mainContentPanel.add(new JScrollPane(checkedInPanel));
        mainContentPanel.add(new JScrollPane(checkedOutPanel));

        // --- 下部パネル ---
        JPanel southPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        
        JButton reserveButton = new JButton("予約作成");
        JButton checkinButton = new JButton("チェックイン");
        JButton checkoutButton = new JButton("チェックアウト");
        JButton cancelButton = new JButton("予約キャンセル");
        JButton refreshButton = new JButton("表示更新");

        Font buttonFont = new Font("SansSerif", Font.PLAIN, 14);
        reserveButton.setFont(buttonFont);
        checkinButton.setFont(buttonFont);
        checkoutButton.setFont(buttonFont);
        cancelButton.setFont(buttonFont);
        refreshButton.setFont(buttonFont);
        
        buttonPanel.add(reserveButton);
        buttonPanel.add(checkinButton);
        buttonPanel.add(checkoutButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(refreshButton);

        statusBar = new JLabel("準備完了");
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(statusBar, BorderLayout.SOUTH);

        // --- 全体をフレームに追加 ---
        Container contentPane = getContentPane();
        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(mainContentPanel, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);

        setupActionListeners(reserveButton, checkinButton, checkoutButton, cancelButton, refreshButton, clearSearchButton);
        refreshDisplay();
    }
    
    private JPanel createGroupPanel(String title) {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14)
        ));
        return panel;
    }

    private void setupActionListeners(JButton reserve, JButton checkin, JButton checkout, JButton cancel, JButton refresh, JButton clearSearch) {
        // (予約、チェックイン等のボタンのリスナーは変更なし)
        reserve.addActionListener(e -> {
            String dateStr = JOptionPane.showInputDialog(this, "到着日を入力 (yyyy/MM/dd):", "予約作成", JOptionPane.PLAIN_MESSAGE);
            if (dateStr == null || dateStr.trim().isEmpty()) return;
            java.util.Date stayingDate = DateUtil.convertToDate(dateStr);
            if (stayingDate == null) { showError("日付の形式が正しくありません。"); return; }
            try {
                ReserveRoomForm form = new ReserveRoomForm();
                form.setStayingDate(stayingDate);
                String reservationNumber = form.submitReservation();
                showStatusMessage("予約完了 (番号: " + reservationNumber + ")");
                refreshDisplay();
            } catch (AppException ex) { showError(ex.getFormattedDetailMessages("\n")); }
        });

        checkin.addActionListener(e -> {
            List<String> choices = dbConnector.getActiveReservationNumbers();
            if (choices.isEmpty()) { showInfo("チェックイン可能な予約がありません。"); return; }
            showChoiceDialog("チェックイン", "チェックインする予約番号を選択:", choices.toArray(new String[0]))
                .ifPresent(this::performCheckIn);
        });

        checkout.addActionListener(e -> {
            List<String> choices = dbConnector.getCheckedInRoomNumbers();
            if (choices.isEmpty()) { showInfo("チェックアウト可能な部屋がありません。"); return; }
            showChoiceDialog("チェックアウト", "チェックアウトする部屋番号を選択:", choices.toArray(new String[0]))
                .ifPresent(this::performCheckOut);
        });
        
        cancel.addActionListener(e -> {
             List<String> choices = dbConnector.getActiveReservationNumbers();
            if (choices.isEmpty()) { showInfo("キャンセル可能な予約がありません。"); return; }
            showChoiceDialog("予約キャンセル", "キャンセルする予約番号を選択:", choices.toArray(new String[0]))
                .ifPresent(this::performCancel);
        });

        refresh.addActionListener(e -> {
            dateSearchField.setText("");
            refreshDisplay();
            showStatusMessage("表示を更新しました。");
        });
        
        // ★★★ 検索関連のイベントリスナーを追加
        java.awt.event.ActionListener sortListener = e -> refreshDisplay();
        sortDescRadio.addActionListener(sortListener);
        sortAscRadio.addActionListener(sortListener);

        dateSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshDisplay(); }
            public void removeUpdate(DocumentEvent e) { refreshDisplay(); }
            public void changedUpdate(DocumentEvent e) { refreshDisplay(); }
        });

        clearSearch.addActionListener(e -> dateSearchField.setText(""));
    }

    public void refreshDisplay() {
        reservedPanel.removeAll();
        checkedInPanel.removeAll();
        checkedOutPanel.removeAll();

        List<ReservationInfo> infoList = dbConnector.getReservationInfoList();

        // ★★★ 日付検索のフィルタリング処理を追加
        String searchDateStr = dateSearchField.getText().trim();
        if (!searchDateStr.isEmpty()) {
            infoList = infoList.stream()
                               .filter(info -> {
                                   String infoDateStr = new SimpleDateFormat("yyyy/MM/dd").format(info.getStayingDate());
                                   return infoDateStr.equals(searchDateStr);
                               })
                               .collect(Collectors.toList());
        }

        // ダッシュボード用カウンター
        int reservedCount = 0;
        int checkedInCount = 0;
        int checkedOutCount = 0;

        // ソート処理
        if (sortDescRadio.isSelected()) {
            infoList.sort(Comparator.comparing(ReservationInfo::getStayingDate).reversed());
        } else {
            infoList.sort(Comparator.comparing(ReservationInfo::getStayingDate));
        }
        
        for (ReservationInfo info : infoList) {
            ReservationSquare square = new ReservationSquare(this, info);
            switch (info.getStatus()) {
                case RESERVED:  reservedPanel.add(square);  reservedCount++; break;
                case CHECKED_IN: checkedInPanel.add(square); checkedInCount++; break;
                case CHECKED_OUT: checkedOutPanel.add(square); checkedOutCount++; break;
            }
        }
        
        reservedCountLabel.setText("予約中: " + reservedCount + "件");
        checkedInCountLabel.setText("チェックイン済: " + checkedInCount + "件");
        checkedOutCountLabel.setText("チェックアウト済: " + checkedOutCount + "件");
        
        if (reservedPanel.getComponentCount() == 0) reservedPanel.add(new JLabel("該当なし"));
        if (checkedInPanel.getComponentCount() == 0) checkedInPanel.add(new JLabel("該当なし"));
        if (checkedOutPanel.getComponentCount() == 0) checkedOutPanel.add(new JLabel("該当なし"));

        revalidate();
        repaint();
    }
    
    public void performCheckIn(String reservationNumber) {
        try {
            CheckInRoomForm form = new CheckInRoomForm();
            form.setReservationNumber(reservationNumber);
            String roomNumber = form.checkIn();
            showStatusMessage("チェックイン完了 (部屋: " + roomNumber + ")");
            refreshDisplay();
        } catch (AppException ex) { showError(ex.getFormattedDetailMessages("\n")); }
    }
    
    public void performCancel(String reservationNumber) {
        try {
            ReserveRoomForm form = new ReserveRoomForm();
            form.setReservationNumber(reservationNumber);
            form.cancelReservation();
            showStatusMessage("予約 " + reservationNumber + " をキャンセルしました。");
            refreshDisplay();
        } catch (AppException ex) { showError("キャンセルに失敗しました。\n" + ex.getFormattedDetailMessages("\n")); }
    }
    
    public void performCheckOut(String roomNumber) {
         try {
            CheckOutRoomForm form = new CheckOutRoomForm();
            form.setRoomNumber(roomNumber);
            form.checkOut();
            showStatusMessage("チェックアウト完了 (部屋: " + roomNumber + ")");
            refreshDisplay();
        } catch (AppException ex) { showError(ex.getFormattedDetailMessages("\n")); }
    }

    private Optional<String> showChoiceDialog(String title, String message, String[] choices) {
        JComboBox<String> comboBox = new JComboBox<>(choices);
        int result = JOptionPane.showConfirmDialog(this, comboBox, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return (result == JOptionPane.OK_OPTION) ? Optional.ofNullable((String) comboBox.getSelectedItem()) : Optional.empty();
    }
    
    public void showStatusMessage(String message) {
        statusBar.setText(message);
        Timer timer = new Timer(5000, e -> statusBar.setText(" "));
        timer.setRepeats(false);
        timer.start();
    }
    
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "情報", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) { e.printStackTrace(); }
        SwingUtilities.invokeLater(() -> new GUI().setVisible(true));
    }
}

// ② 画面表示上の予約状態を定義
enum DisplayStatus { RESERVED, CHECKED_IN, CHECKED_OUT; }

// ③ 表示用のデータクラス
class ReservationInfo {
    private final String displayId;
    private final java.util.Date stayingDate;
    private final DisplayStatus status;
    private final int amount;

    public ReservationInfo(String displayId, java.util.Date stayingDate, DisplayStatus status, int amount) {
        this.displayId = displayId;
        this.stayingDate = stayingDate;
        this.status = status;
        this.amount = amount;
    }

    public String getDisplayId() { return displayId; }
    public java.util.Date getStayingDate() { return stayingDate; }
    public DisplayStatus getStatus() { return status; }
    public int getAmount() { return amount; }
}

// ④ 予約パネル（正方形）のクラス
class ReservationSquare extends JPanel {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    public ReservationSquare(GUI mainFrame, ReservationInfo info) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        setPreferredSize(new Dimension(200, 170));

        switch (info.getStatus()) {
            case RESERVED:  setBackground(new Color(225, 225, 225)); break;
            case CHECKED_IN: setBackground(new Color(230, 255, 230)); break;
            case CHECKED_OUT: setBackground(new Color(60, 63, 65)); break;
        }
        Color textColor = (info.getStatus() == DisplayStatus.CHECKED_OUT) ? Color.WHITE : Color.BLACK;
        Font font = new Font("SansSerif", Font.PLAIN, 13);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(createLabel("日付: " + DATE_FORMAT.format(info.getStayingDate()), textColor, font));
        infoPanel.add(createLabel("番号: " + info.getDisplayId(), textColor, font));
        infoPanel.add(createLabel("状態: " + info.getStatus().name(), textColor, font));

        if (info.getStatus() == DisplayStatus.CHECKED_IN || info.getStatus() == DisplayStatus.CHECKED_OUT) {
            infoPanel.add(createLabel("料金: " + CURRENCY_FORMAT.format(info.getAmount()), textColor, font));
        }
        
        add(infoPanel, BorderLayout.CENTER);

        // ★★★ ここが修正点：パネル内ボタンのロジックを復活 ★★★
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        switch (info.getStatus()) {
            case RESERVED:
                buttonPanel.add(createMiniButton("チェックイン", e -> mainFrame.performCheckIn(info.getDisplayId())));
                buttonPanel.add(createMiniButton("キャンセル", e -> mainFrame.performCancel(info.getDisplayId())));
                break;
            case CHECKED_IN:
                buttonPanel.add(createMiniButton("チェックアウト", e -> mainFrame.performCheckOut(info.getDisplayId())));
                break;
            case CHECKED_OUT:
                break;
        }
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createMiniButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        button.setMargin(new Insets(2, 5, 2, 5));
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return button;
    }
    
    private JLabel createLabel(String text, Color color, Font font) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(font);
        return label;
    }
}

// ⑤ データ取得クラス
class GuiDbConnector {
    private static final String DRIVER_NAME = "org.hsqldb.jdbcDriver";
    private static final String URL = "jdbc:hsqldb:hsql://localhost;shutdown=true";
    private static final String ID = "sa";
    private static final String PASSWORD = "";

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName(DRIVER_NAME);
        return DriverManager.getConnection(URL, ID, PASSWORD);
    }

    public List<String> getActiveReservationNumbers() {
        List<String> numbers = new ArrayList<>();
        String sql = "SELECT RESERVATIONNUMBER FROM RESERVATION WHERE STATUS = 'create'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                numbers.add(rs.getString("RESERVATIONNUMBER"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return numbers;
    }

    public List<String> getCheckedInRoomNumbers() {
        List<String> numbers = new ArrayList<>();
        List<ReservationInfo> allInfo = getReservationInfoList();
        for (ReservationInfo info : allInfo) {
            if (info.getStatus() == DisplayStatus.CHECKED_IN) {
                numbers.add(info.getDisplayId());
            }
        }
        return numbers;
    }

    public List<ReservationInfo> getReservationInfoList() {
        List<ReservationInfo> allInfo = new ArrayList<>();
        String resSql = "SELECT * FROM RESERVATION";
        String paySql = "SELECT * FROM PAYMENT";

        try (Connection conn = getConnection();
             Statement stmt1 = conn.createStatement(); ResultSet resRs = stmt1.executeQuery(resSql);
             Statement stmt2 = conn.createStatement(); ResultSet payRs = stmt2.executeQuery(paySql)) {
            
            while(payRs.next()) {
                String statusStr = payRs.getString("STATUS");
                DisplayStatus status = statusStr.equals(Payment.PAYMENT_STATUS_CONSUME) ? DisplayStatus.CHECKED_OUT : DisplayStatus.CHECKED_IN;
                
                allInfo.add(new ReservationInfo(
                    payRs.getString("ROOMNUMBER"),
                    DateUtil.convertToDate(payRs.getString("STAYINGDATE")),
                    status,
                    payRs.getInt("AMOUNT")
                ));
            }

            while (resRs.next()) {
                if (resRs.getString("STATUS").equals(Reservation.RESERVATION_STATUS_CREATE)) {
                    allInfo.add(new ReservationInfo(
                        resRs.getString("RESERVATIONNUMBER"),
                        DateUtil.convertToDate(resRs.getString("STAYINGDATE")),
                        DisplayStatus.RESERVED,
                        0
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "DB接続に失敗しました。\nHSQLDBが起動しているか確認してください。", "DBエラー", JOptionPane.ERROR_MESSAGE);
        }
        return allInfo;
    }
}

// ⑥ 折り返しレイアウトマネージャー
class WrapLayout extends FlowLayout {
	public WrapLayout() { super(); }
	public WrapLayout(int align) { super(align); }
	public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }
	@Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
	@Override public Dimension minimumLayoutSize(Container target) {
		Dimension minimum = layoutSize(target, false);
		minimum.width -= (getHgap() + 1);
		return minimum;
	}
	private Dimension layoutSize(Container target, boolean preferred) {
		synchronized (target.getTreeLock()) {
			int targetWidth = target.getSize().width;
			Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
			if(scrollPane != null) { targetWidth = ((JScrollPane)scrollPane).getViewport().getWidth(); }
			if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
			int hgap = getHgap();
			int vgap = getVgap();
			Insets insets = target.getInsets();
			int horizontalInsetsAndGaps = insets.left + insets.right + (hgap * 2);
			int maxWidth = targetWidth - horizontalInsetsAndGaps;
			Dimension dim = new Dimension(0, 0);
			int rowWidth = 0;
			int rowHeight = 0;
			int nmembers = target.getComponentCount();
			for (int i = 0; i < nmembers; i++) {
				Component m = target.getComponent(i);
				if (m.isVisible()) {
					Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
					if (rowWidth + d.width > maxWidth) {
						addRow(dim, rowWidth, rowHeight);
						rowWidth = 0;
						rowHeight = 0;
					}
					if (rowWidth != 0) { rowWidth += hgap; }
					rowWidth += d.width;
					rowHeight = Math.max(rowHeight, d.height);
				}
			}
			addRow(dim, rowWidth, rowHeight);
			dim.width += horizontalInsetsAndGaps;
			dim.height += insets.top + insets.bottom + vgap * 2;
			return dim;
		}
	}
	private void addRow(Dimension dim, int rowWidth, int rowHeight) {
		dim.width = Math.max(dim.width, rowWidth);
		if (dim.height > 0) { dim.height += getVgap(); }
		dim.height += rowHeight;
	}
}