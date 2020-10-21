package com.sv.runcmd;

import com.sv.core.DefaultConfigs;
import com.sv.core.MyLogger;
import com.sv.core.Utils;
import com.sv.runcmd.helpers.*;
import com.sv.swingui.*;
import com.sv.swingui.UIConstants.ColorsNFonts;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.sv.core.Constants.*;
import static com.sv.swingui.UIConstants.*;

public class RunCommandUI extends AppFrame {

    private int themeIdx = 0;
    private int colorIdx = 0;

    enum Configs {
        RandomThemes, RandomColors, FavBtnLimit, NumOnFav
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

    private static final int DEFAULT_NUM_ROWS = 10;
    private static final String APP_TITLE = "Run Command";
    private static final String JCB_TOOL_TIP = "Changes every 10 minutes";

    private static String lastCmdRun = "none", lastThemeApplied, lastColorApplied;

    private final MyLogger logger;
    private DefaultConfigs configs;
    private DefaultTableModel model;
    private JLabel lblInfo;
    private AppTable tblCommands;

    private JTextField txtFilter;
    private JButton btnReload, btnClear;
    private JButton[] btnFavs;
    private List<String> favs;
    // Should be either 5 or 10
    private boolean numOnFav = false;
    private int favBtnLimit = 10;
    private final int BTN_IN_A_ROW = 5;
    private final int LBL_INFO_FONT_SIZE = 14;
    private JCheckBox jcbRandomThemes, jcbRandomColor;

    private final String JCB_THEME_TEXT = "random themes";
    private final String JCB_COLOR_TEXT = "random colors";

    private UIManager.LookAndFeelInfo[] lookAndFeels;

    private final RunCommand runCommand;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public RunCommandUI(RunCommand runCommand, MyLogger logger) {
        super(APP_TITLE);
        this.runCommand = runCommand;
        this.logger = logger;
        SwingUtilities.invokeLater(this::initComponents);
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {

        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));

        final int MAX_FAV_ALLOWED = 10;
        final int MIN_FAV_ALLOWED = 5;
        favBtnLimit = configs.getIntConfig(Configs.FavBtnLimit.name());
        // if config value of < 5
        if (favBtnLimit < MIN_FAV_ALLOWED) {
            logger.log("favBtnLimit reset to " + MIN_FAV_ALLOWED);
            favBtnLimit = MIN_FAV_ALLOWED;
        }
        // if config value is between 6 to 9 or greater than 10
        if (favBtnLimit > MIN_FAV_ALLOWED && favBtnLimit != MAX_FAV_ALLOWED) {
            logger.log("favBtnLimit reset to " + MAX_FAV_ALLOWED);
            favBtnLimit = MAX_FAV_ALLOWED;
        }
        numOnFav = configs.getBooleanConfig(Configs.NumOnFav.name());
        logger.log("favBtnLimit = " + favBtnLimit + ", numOnFav = " + numOnFav);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        setIconImage(new ImageIcon("./icons/app-icon.png").getImage());

        lookAndFeels = UIManager.getInstalledLookAndFeels();
        favs = new ArrayList<>();

        //Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

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

        Border lineBorder = new LineBorder(Color.black, 5, true);
        final int TXT_COLS = 20;
        JLabel lblFilter = new JLabel("Filter");
        lblInfo = new JLabel("Welcome");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblInfo.setBorder(lineBorder);
        lblInfo.setOpaque(true);
        lblInfo.setFont(SwingUtils.getCalibriFont(Font.BOLD, LBL_INFO_FONT_SIZE));

        txtFilter = new JTextField(TXT_COLS);
        lblFilter.setLabelFor(txtFilter);
        lblFilter.setDisplayedMnemonic('F');

        btnReload = new AppButton("Reload", 'R');
        btnReload.addActionListener(evt -> reloadFile());
        btnClear = new AppButton("Clear", 'C');
        btnClear.addActionListener(evt -> clearFilter());

        JButton btnExit = new AppExitButton(true);

        createTable();
        btnFavs = new JButton[favBtnLimit];
        for (int i = 0; i < favBtnLimit; i++) {
            String s = "" + (i == 9 ? 0 : i + 1);
            JButton b = new AppButton(s, s.charAt(0));
            b.addActionListener(evt -> execCommand(b.getToolTipText()));
            btnFavs[i] = b;
        }
        redrawFavBtns();

        JPanel favBtnPanel = new JPanel(new GridLayout(favBtnLimit / BTN_IN_A_ROW, 1));
        JPanel favBtnPanel1 = new JPanel(new GridBagLayout());
        JPanel favBtnPanel2 = new JPanel(new GridBagLayout());
        favBtnPanel.add(favBtnPanel1);
        if (btnFavs.length > BTN_IN_A_ROW) {
            favBtnPanel.add(favBtnPanel2);
        }
        TitledBorder titledFP = new TitledBorder("Favourites (starts with *)");
        favBtnPanel.setBorder(titledFP);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        for (int i = 0; i < btnFavs.length; i++) {
            JButton b = btnFavs[i];
            c.gridx++;
            if (i < BTN_IN_A_ROW) {
                favBtnPanel1.add(b, c);
            } else {
                favBtnPanel2.add(b, c);
            }
        }

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        controlPanel.add(jcbRandomThemes);
        controlPanel.add(jcbRandomColor);
        controlPanel.setBorder(EMPTY_BORDER);

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.add(lblInfo);
        topPanel.add(controlPanel);
        topPanel.add(favBtnPanel);
        topPanel.setBorder(EMPTY_BORDER);

        JPanel lowerPanel = new JPanel(new BorderLayout());
        JScrollPane jspCmds = new JScrollPane(tblCommands);

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridBagLayout());
        filterPanel.add(lblFilter);
        filterPanel.add(txtFilter);
        filterPanel.add(btnClear);
        filterPanel.add(btnReload);
        filterPanel.add(btnExit);
        filterPanel.setBorder(EMPTY_BORDER);

        lowerPanel.add(filterPanel, BorderLayout.NORTH);
        lowerPanel.add(jspCmds, BorderLayout.CENTER);
        lowerPanel.setBorder(EMPTY_BORDER);

        parentContainer.add(topPanel, BorderLayout.NORTH);
        parentContainer.add(lowerPanel, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        new Timer().schedule(new ThemeChangerTask(this), 0, MIN_10);
        new Timer().schedule(new ColorChangerTask(this), 0, MIN_1);

        setControlsToEnable();
        setPosition();
    }

    private void setControlsToEnable() {
        List<Component> cmpList = new ArrayList<>();

        cmpList.add(txtFilter);
        cmpList.add(btnClear);
        cmpList.add(btnReload);
        cmpList.add(tblCommands);
        cmpList.addAll(Arrays.asList(btnFavs));

        Component[] components = cmpList.toArray(new Component[0]);
        setComponentToEnable(components);
        //setComponentContrastToEnable(null);
        enableControls();
    }

    private String checkLength(String s) {
        final int BTN_TEXT_LIMIT = 8;
        if (s.length() > BTN_TEXT_LIMIT) {
            return s.substring(0, BTN_TEXT_LIMIT - ELLIPSIS.length()) + ELLIPSIS;
        }
        return s;
    }

    public void changeColor() {
        ColorsNFonts color = jcbRandomColor.isSelected() ? getNextColor() : ColorsNFonts.DEFAULT;
        logger.log("Applying color: " + color.name().toLowerCase());
        lblInfo.setBackground(color.getBk());
        lblInfo.setForeground(color.getFg());
        Font font = getLblInfoFont(color.getFont());
        logger.log("Applying font: " + font.getName());
        lblInfo.setFont(getLblInfoFont(color.getFont()));
        lblInfo.setBorder(new LineBorder(color.getFg(), 3, true));
        lastColorApplied = color.name().toLowerCase();
        updateInfo();
    }

    private Font getLblInfoFont(String font) {
        return new Font(font, Font.BOLD, LBL_INFO_FONT_SIZE);
    }

    private ColorsNFonts getNextColor() {
        if (colorIdx == ColorsNFonts.values().length) {
            colorIdx = 0;
        }
        return ColorsNFonts.values()[colorIdx++];
    }

    public void changeTheme() {
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
        SwingUtils.createEmptyRows(COLS.values().length, DEFAULT_NUM_ROWS, model);
        /*String[] emptyRow = new String[COLS.values().length];
        Arrays.fill(emptyRow, Utils.EMPTY);
        IntStream.range(0, DEFAULT_NUM_ROWS).forEach(i -> model.addRow(emptyRow));*/
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
            if (idx.get() >= favBtnLimit) {
                break;
            }
            JButton b = btnFavs[idx.get()];
            b.setEnabled(true);
            String nm = "" + (numOnFav ? (idx.intValue() == 9 ? 0 : (idx.intValue() + 1)) + " " : "");
            nm += getDisplayName(cmd);
            b.setText(checkLength(nm));
            b.setToolTipText(cmd + ". Shortcut: Alt+" + (idx.intValue() == 9 ? 0 : (idx.intValue() + 1)));
            idx.getAndIncrement();
        }
    }

    // This will be called by reflection from SwingUI jar
    public void handleDblClickOnRow(AppTable table, Object[] p) {
        execCommand(table.getValueAt(table.getSelectedRow(), 0).toString());
    }

    private void setUpSorterAndFilter(RunCommandUI obj, AppTable table,
                                      DefaultTableModel model, JTextField txtFilter) {
        table.addSorter(model);
        table.addFilter(txtFilter);
        table.addDblClickOnRow(obj, new Object[]{});
        table.addEnterOnRow(new RunCommandAction(this, table));
        table.applyChangeListener(txtFilter);
    }

    private void createTable() {
        model = SwingUtils.getTableModel(
                Arrays.stream(COLS.class.getEnumConstants()).map(COLS::getName).toArray(String[]::new)
        );

        createDefaultRows();

        Border borderBlue = new LineBorder(Color.BLUE, 1);
        tblCommands = new AppTable(model);
        tblCommands.setTableHeader(new TableHeaderToolTip(tblCommands.getColumnModel(),
                Arrays.stream(COLS.class.getEnumConstants()).map(COLS::getToolTip).toArray(String[]::new)
        ));
        tblCommands.setBorder(borderBlue);

        setUpSorterAndFilter(this, tblCommands, model, txtFilter);

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

        createRows();
    }

    private void updateInfo() {
        if (lastThemeApplied != null) {
            jcbRandomThemes.setText(JCB_THEME_TEXT + " (" + ELLIPSIS + ")");
            jcbRandomThemes.setToolTipText(JCB_TOOL_TIP + ". Present theme: " + lastThemeApplied);
        }
        if (lastColorApplied != null) {
            jcbRandomColor.setText(JCB_COLOR_TEXT + " (" + lastColorApplied + ")");
            Font f = lblInfo.getFont();
            jcbRandomColor.setToolTipText(JCB_TOOL_TIP + ". Present color: " + lastColorApplied
                    + ", Font: " + f.getName() + "/" + (f.isBold() ? "bold" : "plain") + "/" + f.getSize());
        }
        Font f = lblInfo.getFont();
        lblInfo.setText(lastThemeApplied + ", " + f.getName());
        String tip = "Last Command Run [" + lastCmdRun
                + "] Present theme [" + lastThemeApplied
                + "] Present color [" + lastColorApplied
                + "] Font [" + f.getName() + "/" + (f.isBold() ? "bold" : "plain") + "/" + f.getSize()
                + "]";
        lblInfo.setToolTipText(tip);
        logger.log(tip + ", Thread pool current size: " + threadPool.toString());
    }

    public void runCmdCallable(String cmd) {
        runCommand.execCommand(cmd);
        lastCmdRun = getDisplayName(cmd);
        updateInfo();
        updateTitle(lastCmdRun);
        enableControls();
    }

    public void execCommand(String cmd) {
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
                cmd.substring(cmd.lastIndexOf(SLASH) + SLASH.length());
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

        if (tblCommands.getRowCount() > DEFAULT_NUM_ROWS) {
            setSize(getWidth(), (int) (getHeight() * 1.3));
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

    public String getRandomThemes() {
        return jcbRandomThemes.isSelected() + "";
    }

    public String getRandomColors() {
        return jcbRandomColor.isSelected() + "";
    }

    public String getFavBtnLimit() {
        return favBtnLimit + "";
    }

    public String getNumOnFav() {
        return numOnFav + "";
    }
}

