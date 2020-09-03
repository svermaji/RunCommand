package com.sv.runcmd;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

public class RunCommand extends AppFrame {

    private int themeIdx = 0;
    private int colorIdx = 0;

    public enum COLS {
        IDX(0, "#", "center", 0),
        COMMAND(1, "Command", "left", -1);

        String name, alignment;
        int idx, width;

        COLS(int idx, String name, String alignment, int width) {
            this.name = name;
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
    }

    public enum COLOR {
        CYAN(Color.CYAN, Color.BLACK),
        BLACK(Color.BLACK, Color.GREEN),
        GRAY(Color.GRAY, Color.BLACK),
        WHITE(Color.WHITE, Color.BLUE),
        DEFAULT(Color.lightGray, Color.BLACK);

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

    private static String lastCmdRun, lastThemeApplied, lastColorApplied;

    private MyLogger logger;
    private DefaultConfigs configs;
    private DefaultTableModel model;
    private JLabel lblInfo;
    private JTable tblCommands;

    private JTextField txtFilter;
    private JButton btnReload, btnClear, btnExit;
    private JCheckBox jcbRandomThemes, jcbRandomColor;

    private final String JCB_THEME_TEXT = "random themes";
    private final String JCB_COLOR_TEXT = "random colors";

    private UIManager.LookAndFeelInfo[] lookAndFeels;

    private TableRowSorter<DefaultTableModel> sorter;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        new RunCommand().initComponents();
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {
        logger = MyLogger.createLogger("run-utility.log");

        configs = new DefaultConfigs(logger);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        setIconImage(new ImageIcon("./app-icon.png").getImage());
        setTitle(APP_TITLE);

        lookAndFeels = UIManager.getInstalledLookAndFeels();

        Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

        jcbRandomThemes = new JCheckBox(JCB_THEME_TEXT,
                Boolean.parseBoolean(configs.getConfig(DefaultConfigs.Config.RANDOM_THEMES)));
        jcbRandomThemes.setToolTipText("Changes every 10 minutes");
        jcbRandomThemes.addActionListener(evt -> handleTheme());
        jcbRandomColor = new JCheckBox(JCB_COLOR_TEXT,
                Boolean.parseBoolean(configs.getConfig(DefaultConfigs.Config.RANDOM_COLORS)));
        jcbRandomColor.setToolTipText("Changes every 10 minutes");
        jcbRandomColor.addActionListener(evt -> handleColor());

        Border lineBorder = new LineBorder(Color.black, 1);
        final int TXT_COLS = 20;
        JLabel lblFilter = new JLabel("Filter");
        lblInfo = new JLabel("Welcome");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblInfo.setBorder(lineBorder);
        lblInfo.setOpaque(true);
        lblInfo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        txtFilter = new JTextField(TXT_COLS);
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

        btnExit = new AppExitButton();

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(lblFilter);
        inputPanel.add(txtFilter);
        inputPanel.add(btnClear);
        inputPanel.add(btnReload);
        inputPanel.add(btnExit);
        inputPanel.setBorder(emptyBorder);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.add(jcbRandomThemes);
        controlPanel.add(jcbRandomColor);
        controlPanel.setBorder(emptyBorder);

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.add(lblInfo);
        topPanel.add(inputPanel);
        topPanel.add(controlPanel);
        topPanel.setBorder(emptyBorder);

        createTable();

        JScrollPane jspCmds = new JScrollPane(tblCommands);
        jspCmds.setBorder(emptyBorder);

        parentContainer.add(topPanel, BorderLayout.NORTH);
        parentContainer.add(jspCmds, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        threadPool.submit(new ThemeChangerCallable(this));
        threadPool.submit(new ColorChangerCallable(this));

        setPosition();
    }

    private void handleColor() {
        COLOR color = jcbRandomColor.isSelected() ? getNextColor() : COLOR.DEFAULT;
        logger.log("Applying color: " + color.name().toLowerCase());
        lblInfo.setBackground(color.getBk());
        lblInfo.setForeground(color.getFg());
        lastColorApplied = color.name().toLowerCase();
        updateInfo();
    }

    private COLOR getNextColor() {
        if (colorIdx == COLOR.values().length) {
            colorIdx = 0;
        }
        return COLOR.values()[colorIdx++];
    }

    private void handleTheme() {
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
        clearOldRun();
        createRows();
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
                    runCommand(table.getValueAt(row, 0).toString());
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
        }
        if (lastColorApplied != null) {
            jcbRandomColor.setText(JCB_COLOR_TEXT + " (" + lastColorApplied + ")");
        }
        logger.log("Thread pool current size: " + threadPool.toString());
    }

    private void updateTitle(String subTitle) {
        setTitle(APP_TITLE + " - " + subTitle);
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

    private void runCommand(String cmd) {
        threadPool.submit(new RunCommandCallable(this, cmd));
    }

    private void clearOldRun() {
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
            if (i < DEFAULT_NUM_ROWS) {
                tblCommands.setValueAt(command, i, COLS.IDX.getIdx());
                tblCommands.setValueAt(command, i, COLS.COMMAND.getIdx());
            } else {
                model.addRow(new String[]{command, command});
            }
            i++;
        }
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

        if (tblCommands.getRowCount() > (DEFAULT_NUM_ROWS * 2) / 2) {
            setSize(getWidth(), getHeight() * 2);
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

        private final RunCommand rc;
        private final String cmd;

        public RunCommandCallable(RunCommand rc, String command) {
            this.rc = rc;
            this.cmd = command;
        }

        @Override
        public Boolean call() {
            try {
                String cmdStr = cmd.contains(" (") ? cmd.substring(0, cmd.indexOf(" (")) : cmd;
                rc.logger.log("Calling command [" + cmdStr + "]");
                Runtime.getRuntime().exec(cmdStr);
                lastCmdRun = getCmdInfo();
                rc.updateInfo();
                rc.updateTitle(lastCmdRun);
            } catch (Exception e) {
                rc.logger.error(e);
            }
            return true;
        }

        private String getCmdInfo() {
            String chk = " (";
            return cmd.contains(chk) ?
                    cmd.substring(cmd.indexOf(chk) + chk.length(), cmd.lastIndexOf(")")) :
                    cmd.substring(cmd.lastIndexOf("\\"));
        }
    }

    static class ThemeChangerCallable implements Callable<Boolean> {

        private final RunCommand rc;

        public ThemeChangerCallable(RunCommand rc) {
            this.rc = rc;
        }

        @Override
        public Boolean call() {
            while (true) {
                try {
                    Thread.sleep(THEME_COLOR_CHANGE_TIME);
                } catch (InterruptedException e) {
                    rc.logger.warn("Thread sleep interrupted");
                }
                if (Boolean.parseBoolean(rc.getRandomThemes())) {
                    rc.handleTheme();
                    rc.updateInfo();
                }
            }
        }
    }

    static class ColorChangerCallable implements Callable<Boolean> {

        private final RunCommand rc;

        public ColorChangerCallable(RunCommand rc) {
            this.rc = rc;
        }

        @Override
        public Boolean call() {
            while (true) {
                try {
                    Thread.sleep(THEME_COLOR_CHANGE_TIME);
                } catch (InterruptedException e) {
                    rc.logger.warn("Thread sleep interrupted");
                }
                if (Boolean.parseBoolean(rc.getRandomColors())) {
                    rc.handleColor();
                    rc.updateInfo();
                }
            }
        }
    }

    class RunCommandAction extends AbstractAction {

        private final JTable table;

        public RunCommandAction(JTable table) {
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            runCommand(table.getValueAt(row, 0).toString());
        }
    }

    public String getRandomThemes() {
        return jcbRandomThemes.isSelected() + "";
    }

    public String getRandomColors() {
        return jcbRandomColor.isSelected() + "";
    }
}
