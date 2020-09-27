package com.sv.runcmd;

import com.sv.core.*;
import com.sv.swingui.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

public class RunCommandUI extends AppFrame {

    private int themeIdx = 0;
    private int colorIdx = 0;

    enum Configs {
        RandomThemes, RandomColors
    }

    public enum COLS {
        IDX(0, "#", "", "center", 0),
        COMMAND(1, "Commands", "Double click on row OR select & Enter", "left", -1);

        String name, alignment, toolTip;
        int idx, width;

        COLS(int idx, String name, String toolTip, String alignment, int width) {
            this.name = name;
            this.toolTip = toolTip;
            this.idx = idx;
            this.alignment = alignment;
            this.width = width;
        }

        public String getName() {
            return name;
        }

        public int getIdx() {
            return idx;
        }

        public String getAlignment() {
            return alignment;
        }

        public int getWidth() {
            return width;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    public enum COLOR {
        CYAN(Color.CYAN, Color.BLACK),
        BLACK(Color.BLACK, Color.GREEN),
        GRAY(Color.GRAY, Color.WHITE),
        WHITE(Color.WHITE, Color.BLUE),
        MAGENTA(Color.MAGENTA, Color.YELLOW),
        ORANGE(Color.ORANGE, Color.WHITE),
        DEFAULT(Color.LIGHT_GRAY, Color.BLACK);

        Color bk, fg;

        COLOR(Color bk, Color fg) {
            this.bk = bk;
            this.fg = fg;
        }

        public Color getBk() {
            return bk;
        }

        public Color getFg() {
            return fg;
        }
    }

    private static final long THEME_COLOR_CHANGE_TIME = TimeUnit.MINUTES.toMillis(10);
    private static final int DEFAULT_NUM_ROWS = 10;
    private static final String APP_TITLE = "Run Command";
    private static final String JCB_TOOL_TIP = "Changes every 10 minutes";

    private static String lastCmdRun, lastThemeApplied, lastColorApplied;

    private final MyLogger logger;
    private DefaultConfigs configs;
    private DefaultTableModel model;
    private JLabel lblInfo;
    private JTable tblCommands;

    private JTextField txtFilter;
    private JButton btnReload, btnClear;
    private JButton[] btnFavs;
    private List<String> favs;
    private final int FAV_BTN_LIMIT = 5;
    private JCheckBox jcbRandomThemes, jcbRandomColor;

    private final String JCB_THEME_TEXT = "random themes";
    private final String JCB_COLOR_TEXT = "random colors";

    private UIManager.LookAndFeelInfo[] lookAndFeels;

    private TableRowSorter<DefaultTableModel> sorter;

    private final RunCommand runCommand;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public RunCommandUI(RunCommand runCommand, MyLogger logger) {
        this.runCommand = runCommand;
        this.logger = logger;
        initComponents();
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {

        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        setIconImage(new ImageIcon("./app-icon.png").getImage());
        setTitle(APP_TITLE);

        lookAndFeels = UIManager.getInstalledLookAndFeels();
        favs = new ArrayList<>();

        Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

        jcbRandomThemes = new JCheckBox(JCB_THEME_TEXT,
                Boolean.parseBoolean(configs.getConfig(Configs.RandomThemes.name())));
        jcbRandomThemes.setToolTipText(JCB_TOOL_TIP);
        jcbRandomThemes.addActionListener(evt -> changeTheme());
        jcbRandomThemes.setMnemonic('T');
        jcbRandomColor = new JCheckBox(JCB_COLOR_TEXT,
                Boolean.parseBoolean(configs.getConfig(Configs.RandomColors.name())));
        jcbRandomColor.setToolTipText(JCB_TOOL_TIP);
        jcbRandomColor.addActionListener(evt -> changeColor());
        jcbRandomColor.setMnemonic('O');

        Border lineBorder = new LineBorder(Color.black, 1, true);
        final int TXT_COLS = 20;
        JLabel lblFilter = new JLabel("Filter");
        lblInfo = new JLabel("Welcome");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblInfo.setBorder(lineBorder);
        lblInfo.setOpaque(true);
        lblInfo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));

        txtFilter = new JTextField(TXT_COLS);
        lblFilter.setLabelFor(txtFilter);
        lblFilter.setDisplayedMnemonic('F');
        txtFilter.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        addFilter();
                    }

                    public void insertUpdate(DocumentEvent e) {
                        addFilter();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        addFilter();
                    }
                });
        btnReload = new AppButton("Reload", 'R');
        btnReload.addActionListener(evt -> reloadFile());
        btnClear = new AppButton("Clear", 'C');
        btnClear.addActionListener(evt -> clearFilter());

        JButton btnExit = new AppExitButton(true);

        createTable();
        btnFavs = new JButton[FAV_BTN_LIMIT];
        for (int i = 0; i < FAV_BTN_LIMIT; i++) {
            btnFavs[i] = new JButton();
        }
        redrawFavBtns();

        JPanel favBtnPanel = new JPanel(new GridBagLayout());
        TitledBorder titledFP = new TitledBorder("Favourites (starts with *)");
        favBtnPanel.setBorder(titledFP);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        for (JButton b : btnFavs) {
            c.gridx++;
            favBtnPanel.add(b, c);
        }

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.add(jcbRandomThemes);
        controlPanel.add(jcbRandomColor);
        controlPanel.setBorder(emptyBorder);

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.add(lblInfo);
        topPanel.add(controlPanel);
        topPanel.add(favBtnPanel);
        topPanel.setBorder(emptyBorder);

        JPanel lowerPanel = new JPanel(new BorderLayout());
        JScrollPane jspCmds = new JScrollPane(tblCommands);

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());
        filterPanel.add(lblFilter);
        filterPanel.add(txtFilter);
        filterPanel.add(btnClear);
        filterPanel.add(btnReload);
        filterPanel.add(btnExit);
        filterPanel.setBorder(emptyBorder);

        lowerPanel.add(filterPanel, BorderLayout.NORTH);
        lowerPanel.add(jspCmds, BorderLayout.CENTER);
        lowerPanel.setBorder(emptyBorder);

        parentContainer.add(topPanel, BorderLayout.NORTH);
        parentContainer.add(lowerPanel, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        new Timer().schedule(new ThemeChangerTask(this), 0, THEME_COLOR_CHANGE_TIME);
        new Timer().schedule(new ColorChangerTask(this), 0, THEME_COLOR_CHANGE_TIME);

        /*threadPool.submit(new ThemeChangerCallable(this));
        threadPool.submit(new ColorChangerCallable(this));*/

        setPosition();
    }

    private void updateControls(boolean enable) {
        txtFilter.setEnabled(enable);
        btnClear.setEnabled(enable);
        btnReload.setEnabled(enable);
        for (JButton b : btnFavs) {
            if (!b.getText().equalsIgnoreCase("X")) {
                b.setEnabled(enable);
            }
        }
        tblCommands.setEnabled(enable);
    }

    private void enableControls() {
        updateControls(true);
    }

    private void disableControls() {
        updateControls(false);
    }

    private String checkLength(String s) {
        final int BTN_TEXT_LIMIT = 8;
        if (s.length() > BTN_TEXT_LIMIT) {
            return s.substring(0, BTN_TEXT_LIMIT - Utils.ELLIPSIS.length()) + Utils.ELLIPSIS;
        }
        return s;
    }

    private void changeColor() {
        COLOR color = jcbRandomColor.isSelected() ? getNextColor() : COLOR.DEFAULT;
        logger.log("Applying color: " + color.name().toLowerCase());
        lblInfo.setBackground(color.getBk());
        lblInfo.setForeground(color.getFg());
        lblInfo.setBorder(new LineBorder(color.getFg(), 1, true));
        lastColorApplied = color.name().toLowerCase();
        updateInfo();
    }

    private COLOR getNextColor() {
        if (colorIdx == COLOR.values().length - 1) {
            colorIdx = 0;
        }
        return COLOR.values()[colorIdx++];
    }

    private void changeTheme() {
        try {
            if (jcbRandomThemes.isSelected()) {
                String lfClass = getNextLookAndFeel();
                logger.log("Applying look and feel: " + lfClass);
                UIManager.setLookAndFeel(lfClass);
                lastThemeApplied = lfClass.substring(lfClass.lastIndexOf(".") + 1);
            } else {
                String lfClass = UIManager.getSystemLookAndFeelClassName();
                logger.log("Applying system look and feel: " + lfClass);
                UIManager.setLookAndFeel(lfClass);
                lastThemeApplied = lfClass.substring(lfClass.lastIndexOf(".") + 1);
            }
            repaint();
            updateInfo();
        } catch (IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException | ClassNotFoundException e) {
            logger.warn("Unable to apply look and feel");
        }
    }

    private String getNextLookAndFeel() {
        if (themeIdx == lookAndFeels.length) {
            themeIdx = 0;
        }
        return lookAndFeels[themeIdx++].getClassName();
    }

    private void clearFilter() {
        txtFilter.setText("");
    }

    private void createDefaultRows() {
        String[] emptyRow = new String[COLS.values().length];
        Arrays.fill(emptyRow, Utils.EMPTY);
        IntStream.range(0, DEFAULT_NUM_ROWS).forEach(i -> model.addRow(emptyRow));
    }

    private void reloadFile() {
        disableControls();
        clearFilter();
        favs = new ArrayList<>();
        clearOldRows();
        createRows();
        redrawFavBtns();
        enableControls();
    }

    private void cleanFavBtns() {
        for (JButton b : btnFavs) {
            b.setText("X");
            b.setToolTipText("");
            b.setEnabled(false);
        }
    }

    private void redrawFavBtns() {
        cleanFavBtns();
        AtomicInteger idx = new AtomicInteger();
        for (String cmd : favs) {
            if (idx.get() >= FAV_BTN_LIMIT) {
                break;
            }
            JButton b = btnFavs[idx.get()];
            b.setEnabled(true);
            b.setText(checkLength(getDisplayName(cmd)));
            b.setToolTipText(cmd);
            if (b.getActionListeners() != null && b.getActionListeners().length == 0) {
                b.addActionListener(evt -> execCommand(b.getToolTipText()));
            }
            idx.getAndIncrement();
        }
    }

    private void createTable() {
        model = new DefaultTableModel() {

            @Override
            public int getColumnCount() {
                return COLS.values().length;
            }

            @Override
            public String getColumnName(int index) {
                return COLS.values()[index].getName();
            }

        };

        createDefaultRows();

        Border borderBlue = new LineBorder(Color.BLUE, 1);
        tblCommands = new JTable(model);
        tblCommands.setTableHeader(new RunTableHeaders(tblCommands.getColumnModel()));
        tblCommands.setBorder(borderBlue);

        sorter = new TableRowSorter<>(model);
        tblCommands.setRowSorter(sorter);

        addFilter();

        // For making contents non editable
        tblCommands.setDefaultEditor(Object.class, null);

        tblCommands.setAutoscrolls(true);
        tblCommands.setPreferredScrollableViewportSize(tblCommands.getPreferredSize());
        // PATH col contains tooltip

        CellRendererLeftAlign leftRenderer = new CellRendererLeftAlign();
        CellRendererCenterAlign centerRenderer = new CellRendererCenterAlign();

        for (COLS col : COLS.values()) {
            tblCommands.getColumnModel().getColumn(col.getIdx()).setCellRenderer(
                    col.getAlignment().equals("center") ? centerRenderer : leftRenderer);

            if (col.getWidth() != -1) {
                tblCommands.getColumnModel().getColumn(col.getIdx()).setMinWidth(col.getWidth());
                tblCommands.getColumnModel().getColumn(col.getIdx()).setMaxWidth(col.getWidth());
            }
        }

        tblCommands.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    execCommand(table.getValueAt(row, 0).toString());
                }
            }
        });

        InputMap im = tblCommands.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Action.RunCmdCell");
        ActionMap am = tblCommands.getActionMap();
        am.put("Action.RunCmdCell", new RunCommandAction(tblCommands));

        createRows();
    }

    private void updateInfo() {
        if (lastCmdRun != null) {
            lblInfo.setText("Last command tried: " + lastCmdRun);
        }
        if (lastThemeApplied != null) {
            jcbRandomThemes.setText(JCB_THEME_TEXT + " (" + lastThemeApplied + ")");
            jcbRandomThemes.setToolTipText(JCB_TOOL_TIP + ". Present theme: " + lastThemeApplied);
        }
        if (lastColorApplied != null) {
            jcbRandomColor.setText(JCB_COLOR_TEXT + " (" + lastColorApplied + ")");
            jcbRandomColor.setToolTipText(JCB_TOOL_TIP + ". Present color: " + lastColorApplied);
        }
        logger.log("Thread pool current size: " + threadPool.toString());
    }

    private void updateTitle(String subTitle) {
        setTitle(APP_TITLE + Utils.SP_DASH_SP + subTitle);
    }

    private void addFilter() {
        RowFilter<DefaultTableModel, Object> rf;
        try {
            rf = RowFilter.regexFilter(txtFilter.getText(), 0);
        } catch (PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }

    private void execCommand(String cmd) {
        disableControls();
        threadPool.submit(new RunCommandCallable(this, cmd));
    }

    private void clearOldRows() {
        //empty table
        IntStream.range(0, tblCommands.getRowCount()).forEach(i -> model.removeRow(0));
        createDefaultRows();
    }

    private void createRows() {
        List<String> commands = readCommands();

        if (commands == null) {
            throw new AppException("Commands are null.  No command to run.");
        }

        int i = 0;
        for (String command : commands) {
            if (command.startsWith("*")) {
                favs.add(command);
            }
            if (i < DEFAULT_NUM_ROWS) {
                tblCommands.setValueAt(command, i, COLS.IDX.getIdx());
                tblCommands.setValueAt(command, i, COLS.COMMAND.getIdx());
            } else {
                model.addRow(new String[]{command, command});
            }
            i++;
        }
    }

    private String getDisplayName(String cmd) {
        String chk = " (";
        return cmd.contains(chk) ?
                cmd.substring(cmd.indexOf(chk) + chk.length(), cmd.lastIndexOf(")")) :
                cmd.substring(cmd.lastIndexOf(Utils.SLASH) + Utils.SLASH.length());
    }

    private List<String> readCommands() {
        try {
            return Files.readAllLines(Paths.get("./commands.config"));
        } catch (IOException e) {
            logger.error("Error in loading commands.");
        }
        return null;
    }

    private void setPosition() {
        // Setting to right most position
        pack();

        GraphicsConfiguration config = getGraphicsConfiguration();
        Rectangle bounds = config.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);

        if (tblCommands.getRowCount() > (DEFAULT_NUM_ROWS * 1.75) / 2) {
            setSize(getWidth(), (int) (getHeight() * 1.75));
        }

        int x = bounds.x + bounds.width - insets.right - getWidth();
        int y = bounds.y + insets.top + 10;

        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Exit the Application
     */
    private void exitForm() {
        configs.saveConfig(this);
        setVisible(false);
        dispose();
        logger.dispose();
        System.exit(0);
    }

    static class RunCommandCallable implements Callable<Boolean> {

        private final RunCommandUI rc;
        private final String cmd;

        public RunCommandCallable(RunCommandUI rc, String command) {
            this.rc = rc;
            this.cmd = command;
        }

        @Override
        public Boolean call() {
            rc.runCommand.execCommand(cmd);
            lastCmdRun = rc.getDisplayName(cmd);
            rc.updateInfo();
            rc.updateTitle(lastCmdRun);
            rc.enableControls();
            return true;
        }

    }

    //static class ThemeChangerCallable implements Callable<Boolean> {
    static class ThemeChangerTask extends TimerTask {

        private final RunCommandUI rc;

        //public ThemeChangerCallable(RunCommandUI rc) {
        public ThemeChangerTask(RunCommandUI rc) {
            this.rc = rc;
        }

        /*@Override
        public Boolean call() {*/
        @Override
        public void run() {
            if (Boolean.parseBoolean(rc.getRandomThemes())) {
                rc.changeTheme();
            }
            /*while (true) {
                try {
                    Thread.sleep(THEME_COLOR_CHANGE_TIME);
                } catch (InterruptedException e) {
                    rc.logger.warn("Thread sleep interrupted");
                }
                if (Boolean.parseBoolean(rc.getRandomThemes())) {
                    rc.changeTheme();
                }
            }*/
        }
    }

    //static class ColorChangerCallable implements Callable<Boolean> {
    static class ColorChangerTask extends TimerTask {

        private final RunCommandUI rc;

        //public ColorChangerCallable(RunCommandUI rc) {
        public ColorChangerTask(RunCommandUI rc) {
            this.rc = rc;
        }

        /*@Override
        public Boolean call() {*/
        @Override
        public void run() {
            if (Boolean.parseBoolean(rc.getRandomColors())) {
                rc.changeColor();
            }
            /*while (true) {
                try {
                    Thread.sleep(THEME_COLOR_CHANGE_TIME);
                } catch (InterruptedException e) {
                    rc.logger.warn("Thread sleep interrupted");
                }
                if (Boolean.parseBoolean(rc.getRandomColors())) {
                    rc.changeColor();
                }
            }*/
        }
    }

    class RunCommandAction extends AbstractAction {

        private final JTable table;

        public RunCommandAction(JTable table) {
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            execCommand(table.getValueAt(table.getSelectedRow(), 0).toString());
        }
    }

    public String getRandomThemes() {
        return jcbRandomThemes.isSelected() + "";
    }

    public String getRandomColors() {
        return jcbRandomColor.isSelected() + "";
    }

    static class RunTableHeaders extends JTableHeader {

        public RunTableHeaders(TableColumnModel columnModel) {
            super(columnModel);
        }

        public String getToolTipText(MouseEvent e) {
            Point p = e.getPoint();
            int index = columnModel.getColumnIndexAtX(p.x);
            int realIndex = columnModel.getColumn(index).getModelIndex();
            return COLS.values()[realIndex].getToolTip();
        }
    }
}

