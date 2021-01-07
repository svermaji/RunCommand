package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.exception.AppException;
import com.sv.core.logger.MyLogger;
import com.sv.runcmd.helpers.*;
import com.sv.swingui.SwingUtils;
import com.sv.swingui.UIConstants.ColorsNFonts;
import com.sv.swingui.component.AppButton;
import com.sv.swingui.component.AppExitButton;
import com.sv.swingui.component.AppFrame;
import com.sv.swingui.component.table.AppTable;
import com.sv.swingui.component.table.CellRendererCenterAlign;
import com.sv.swingui.component.table.CellRendererLeftAlign;

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
        RandomThemes, RandomColors, ColorIndex, ThemeIndex, FavBtnLimit, NumOnFav
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

    private JCheckBoxMenuItem jcbRT, jcbRC;
    private JMenuBar mbarSettings;
    private JTextField txtFilter;
    private JButton btnReload, btnClear;
    private JButton[] btnFavs;
    private List<String> favs;
    // Should be either 5 or 10
    private boolean numOnFav = false;
    private int favBtnLimit = 10;
    private final int BTN_IN_A_ROW = 5;
    private final int LBL_INFO_FONT_SIZE = 14;

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
        themeIdx = configs.getIntConfig(Configs.ThemeIndex.name());
        colorIdx = configs.getIntConfig(Configs.ColorIndex.name());
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
        lookAndFeels = SwingUtils.getAvailableLAFs();

        logger.log(String.format(
                "favBtnLimit [%s], numOnFav [%s], themeIdx [%s], colorIdx [%s], look-n-Feel count [%s]",
                favBtnLimit, numOnFav, themeIdx, colorIdx, lookAndFeels.length));

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        favs = new ArrayList<>();

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

        createAppMenu();

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(lblInfo);
        //topPanel.add(controlPanel);
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

        setControlsToEnable();
        setPosition();

        new Timer().schedule(new ColorChangerTask(this), SEC_1, MIN_10);
        new Timer().schedule(new ThemeChangerTask(this), SEC_1, MIN_10);

        if (!Boolean.parseBoolean(getRandomThemes())) {
            changeTheme();
        }
        if (!Boolean.parseBoolean(getRandomColors())) {
            changeColor();
        }
    }

    private void createAppMenu() {
        mbarSettings = new JMenuBar();
        JMenu menuSettings = new JMenu("Settings");

        char ch = 's';
        menuSettings.setMnemonic(ch);
        menuSettings.setToolTipText("Settings." + SHORTCUT + ch);

        jcbRT = new JCheckBoxMenuItem("Random themes",
                configs.getBooleanConfig(Configs.RandomThemes.name()));
        jcbRT.setToolTipText(JCB_TOOL_TIP);
        jcbRT.setSelected(configs.getBooleanConfig(Configs.RandomThemes.name()));
        jcbRT.addActionListener(evt -> {
            if (jcbRT.isSelected()) {
                changeTheme();
            }
        });
        jcbRT.setMnemonic('T');

        jcbRC = new JCheckBoxMenuItem("Random colors and fonts",
                configs.getBooleanConfig(Configs.RandomColors.name()));
        jcbRC.setToolTipText(JCB_TOOL_TIP);
        jcbRC.setSelected(configs.getBooleanConfig(Configs.RandomColors.name()));
        jcbRC.addActionListener(evt -> {
            if (jcbRC.isSelected()) {
                changeColor();
            }
        });
        jcbRC.setMnemonic('r');

        menuSettings.add(jcbRC);
        menuSettings.add(SwingUtils.getColorsMenu(true, true,
                false, true, false, this, logger));
        menuSettings.addSeparator();
        menuSettings.add(jcbRT);
        menuSettings.add(SwingUtils.getThemesMenu(this, logger));

        mbarSettings.add(menuSettings);

        setJMenuBar(mbarSettings);
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

    private void applyColor(ColorsNFonts color) {
        //logger.log("Applying color: " + color.name().toLowerCase());
        lblInfo.setBackground(color.getBk());
        lblInfo.setForeground(color.getFg());
        Font font = getLblInfoFont(color.getFont());
        logger.log("Applying color and font: " + font.getName());
        lblInfo.setFont(getLblInfoFont(color.getFont()));
        lblInfo.setBorder(new LineBorder(color.getFg(), 3, true));
        lastColorApplied = color.name().toLowerCase();
        updateInfo();
    }

    private Font getLblInfoFont(String font) {
        return new Font(font, Font.BOLD, LBL_INFO_FONT_SIZE);
    }

    // called from timer
    public void changeColor() {
        applyColor(getNextColor());
    }

    // called from timer
    public void changeTheme() {
        applyTheme(getNextLookAndFeel());
    }

    private ColorsNFonts getNextColor() {
        if (jcbRC.isSelected()) {
            colorIdx++;
            if (colorIdx == ColorsNFonts.values().length) {
                colorIdx = 0;
            }
        }
        return getColor();
    }

    private ColorsNFonts getColor() {
        return ColorsNFonts.values()[colorIdx];
    }

    // This will be called by reflection from SwingUI jar
    public void colorChange(Integer x) {
        colorIdx = x;
        applyColor(ColorsNFonts.values()[colorIdx]);
    }

    // This will be called by reflection from SwingUI jar
    public void themeApplied(Integer x, UIManager.LookAndFeelInfo lnf, Boolean themeApplied) {
        if (themeApplied) {
            themeIdx = x;
            logThemeChangeInfo(lnf);
        }
    }

    private void logThemeChangeInfo(UIManager.LookAndFeelInfo lfClass) {
        lastThemeApplied = lfClass.getName();
        updateInfo();
    }

    private void applyTheme(UIManager.LookAndFeelInfo lfClass) {
        SwingUtils.applyTheme(themeIdx, lfClass, this, logger);
        SwingUtils.updateForTheme(tblCommands);
        //logThemeChangeInfo (lfClass);
    }

    private UIManager.LookAndFeelInfo getNextLookAndFeel() {
        if (jcbRT.isSelected()) {
            themeIdx++;
            if (themeIdx == lookAndFeels.length) {
                themeIdx = 0;
            }
        }
        return getLookAndFeel();
    }

    private UIManager.LookAndFeelInfo getLookAndFeel() {
        return lookAndFeels[themeIdx];
    }

    private void clearFilter() {
        txtFilter.setText("");
    }

    private void createDefaultRows() {
        SwingUtils.createEmptyRows(COLS.values().length, DEFAULT_NUM_ROWS, model);
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
            jcbRT.setToolTipText(JCB_TOOL_TIP + ". Present theme: " + lastThemeApplied);
        }
        if (lastColorApplied != null) {
            Font f = lblInfo.getFont();
            jcbRC.setToolTipText(JCB_TOOL_TIP + ". Font: " + f.getName() + "/" + (f.isBold() ? "bold" : "plain") + "/" + f.getSize());
        }
        Font f = lblInfo.getFont();

        String dispCmd = "none";
        String cmdRun = "Welcome";
        if (!lastCmdRun.equals("none")) {
            dispCmd = getDisplayName(lastCmdRun);
            int charsToDisp = 20;
            cmdRun = (dispCmd.length() > charsToDisp ? dispCmd.substring(0, charsToDisp)
                    + ELLIPSIS : dispCmd) + addTimeString();
        }
        String tip1 = "Executed [" + dispCmd + addTimeString() + "]";
        String tip2 = "Theme [" + lastThemeApplied
                + "] Font [" + f.getName() + "/" + (f.isBold() ? "bold" : "plain") + "/" + f.getSize()
                + "]";
        String tip = tip1 + SPACE + tip2;
        lblInfo.setToolTipText(tip);

        String txt = HTML_STR + CENTER_STR + cmdRun + BR + BR +
                "<span style='font-size:9px'>" + tip2 + SPAN_END + CENTER_END + HTML_END;
        lblInfo.setText(txt);

        //logger.log(tip + ", Thread pool current size: " + threadPool.toString());
        logger.log(tip);
    }

    public void runCmdCallable(String cmd) {
        String msg = runCommand.execCommand(cmd);
        if (!Utils.hasValue(msg)) {
            lastCmdRun = cmd;
            updateInfo();
            //updateTitle(lastCmdRun);
        } else {
            lblInfo.setText(HTML_STR + CENTER_STR + "Error: " + msg + CENTER_END + HTML_END);
        }
        enableControls();
    }

    private String addTimeString() {
        return " @ " + Utils.getTimeNoSec();
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
        return jcbRT.isSelected() + "";
    }

    public String getRandomColors() {
        return jcbRC.isSelected() + "";
    }

    public String getColorIndex() {
        return colorIdx + "";
    }

    public String getThemeIndex() {
        return themeIdx + "";
    }

    public String getFavBtnLimit() {
        return favBtnLimit + "";
    }

    public String getNumOnFav() {
        return numOnFav + "";
    }
}

