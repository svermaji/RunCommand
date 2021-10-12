package com.sv.runcmd;

import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.exception.AppException;
import com.sv.core.logger.MyLogger;
import com.sv.runcmd.helpers.*;
import com.sv.swingui.KeyActionDetails;
import com.sv.swingui.SwingUtils;
import com.sv.swingui.UIConstants;
import com.sv.swingui.component.*;
import com.sv.swingui.component.table.AppTable;
import com.sv.swingui.component.table.CellRendererCenterAlign;
import com.sv.swingui.component.table.CellRendererLeftAlign;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.sv.core.Constants.*;
import static com.sv.swingui.UIConstants.*;

public class RunCommandUI extends AppFrame {

    enum AppProps {
        Host
    }

    enum Configs {
        RandomThemes, RandomColors, ColorIndex, ThemeIndex, FavBtnLimit, NumOnFav,
        RecentFilters, Filter, AutoLock, ApplyColorToApp, ShowFullCmd, CloseCommand
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

    private static final long MIN_15 = MIN_1 * 15;
    private static final long MIN_30 = MIN_1 * 30;
    private static final int DEFAULT_NUM_ROWS = 10;
    private static final String APP_TITLE = "Run Command";
    private static final String JCB_TOOL_TIP = "Changes every 10 minutes";

    private static String lastCmdRun = "none", lastThemeApplied, lastColorApplied;

    private int themeIdx = 0;
    private int colorIdx = 0;
    private final String SEPARATOR = "~";
    public static final int RECENT_LIMIT = 10;
    private String recentFiltersStr, closeCommandStr;
    private final String TXT_F_MAP_KEY = "Action.FilterMenuItem";
    private final RunCommandUtil commandUtil;
    private RunCommandTimer runCommandTimer;
    private TitledBorder titledFP;
    private List<JComponent> toColor;
    private Timer cmdTimer, cmdTimerTrack;
    private JMenu menuRFilters;
    private JMenuBar mb;
    private String timerTrack = "";
    private String FAV_HEADING = "Favourites (starts with *)";

    private final MyLogger logger;
    private DefaultConfigs configs;
    private Properties appProps;
    private DefaultTableModel model;
    private JLabel lblInfo;
    private AppTable tblCommands;
    private JPopupMenu tblRowsPopupMenu = new JPopupMenu();

    private JCheckBoxMenuItem jcbRT, jcbRC, jcbmiApplyToApp, jcbmiAutoLock, jcbmiShowFullCmd;
    private static Color highlightColor, highlightTextColor, selectionColor, selectionTextColor;
    private JMenuBar mbarSettings;
    private JMenu menuTime;
    private AppTextField txtFilter;
    private AppLabel lblFilter;
    private JButton btnReload, btnClear, btnLock, btnChangePwd;
    private JButton[] btnFavs;
    private JLabel[] lblRecents;
    private JPanel favBtnPanel;
    private List<String> favs;
    // Should be either 5 or 10
    private boolean numOnFav = false;
    private boolean showFullCmd = true;
    private int favBtnLimit = 10;
    private int recentLblLimit = 5;
    private final int BTN_IN_A_ROW = 5;
    private final int LBL_INFO_FONT_SIZE = 14;

    private UIManager.LookAndFeelInfo[] lookAndFeels;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public RunCommandUI(MyLogger logger) {
        super(APP_TITLE);
        this.logger = logger;
        this.commandUtil = new RunCommandUtil(logger);
        this.cmdTimer = new Timer("Timer");
        this.cmdTimerTrack = new Timer("TimerTrack");
        SwingUtilities.invokeLater(this::initComponents);
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {

        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));
        appProps = Utils.readPropertyFile("./app.properties", logger);

        super.setLogger(logger);

        showFullCmd = configs.getBooleanConfig(Configs.ShowFullCmd.name());
        logger.info("showFullCmd " + Utils.addBraces(showFullCmd));

        final int MAX_FAV_ALLOWED = 10;
        final int MIN_FAV_ALLOWED = 5;
        favBtnLimit = configs.getIntConfig(Configs.FavBtnLimit.name());
        themeIdx = configs.getIntConfig(Configs.ThemeIndex.name());
        colorIdx = configs.getIntConfig(Configs.ColorIndex.name());
        recentFiltersStr = getCfg(Configs.RecentFilters);
        if (!recentFiltersStr.startsWith(SEPARATOR)) {
            recentFiltersStr = SEPARATOR + recentFiltersStr;
        }
        if (!recentFiltersStr.endsWith(SEPARATOR)) {
            recentFiltersStr = recentFiltersStr + SEPARATOR;
        }
        closeCommandStr = getCfg(Configs.CloseCommand);

        toColor = new ArrayList<>();

        // if config value of < 5
        if (favBtnLimit < MIN_FAV_ALLOWED) {
            logger.info("favBtnLimit reset to " + MIN_FAV_ALLOWED);
            favBtnLimit = MIN_FAV_ALLOWED;
        }
        // if config value is between 6 to 9 or greater than 10
        if (favBtnLimit > MIN_FAV_ALLOWED && favBtnLimit != MAX_FAV_ALLOWED) {
            logger.info("favBtnLimit reset to " + MAX_FAV_ALLOWED);
            favBtnLimit = MAX_FAV_ALLOWED;
        }
        numOnFav = configs.getBooleanConfig(Configs.NumOnFav.name());
        lookAndFeels = SwingUtils.getAvailableLAFs();

        logger.info(String.format(
                "favBtnLimit [%s], numOnFav [%s], themeIdx [%s], colorIdx [%s], look-n-Feel count [%s]",
                favBtnLimit, numOnFav, themeIdx, colorIdx, lookAndFeels.length));

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        favs = new ArrayList<>();

        UIName uin = UIName.LBL_FILTER;
        Border lineBorder = new LineBorder(Color.black, 5, true);
        final int TXT_COLS = 15;
        lblInfo = new JLabel("Welcome");
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        lblInfo.setBorder(lineBorder);
        lblInfo.setOpaque(true);
        lblInfo.setFont(SwingUtils.getCalibriFont(Font.BOLD, LBL_INFO_FONT_SIZE));

        String tip = "Alt + ↓ to select first row in table";
        // setting value from config at last to apply filter
        txtFilter = new AppTextField("", TXT_COLS, getFilters());
        txtFilter.setToolTipText(tip);
        lblFilter = new AppLabel(uin.name, txtFilter, uin.mnemonic, tip);

        uin = UIName.BTN_RELOAD;
        btnReload = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnReload.addActionListener(evt -> reloadFile());
        uin = UIName.BTN_CLEAR;
        btnClear = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnClear.addActionListener(evt -> clearFilter());
        uin = UIName.BTN_LOCK;
        btnLock = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnLock.addActionListener(evt -> showLockScreen(highlightColor));
        uin = UIName.BTN_CHNG_PWD;
        btnChangePwd = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnChangePwd.addActionListener(evt -> showChangePwdScreen(highlightColor));

        uin = UIName.LBL_R_FILTERS;
        mb = new JMenuBar();
        menuRFilters = new JMenu(uin.name);
        mb.setBackground(Color.lightGray);
        menuRFilters.setMnemonic(uin.mnemonic);
        menuRFilters.setToolTipText(uin.tip);
        mb.add(menuRFilters);
        updateRecentMenu(menuRFilters, getRecentFiltersList(), txtFilter, TXT_F_MAP_KEY);

        AppToolBar jtb = new AppToolBar();
        jtb.add(txtFilter);
        jtb.add(mb);
        jtb.add(btnReload);
        jtb.add(btnLock);
        jtb.add(btnChangePwd);

        JButton btnExit = new AppExitButton(true);

        createTable();
        uin = UIName.MNU_COPY;
        JMenuItem tblRowsMICopy = new JMenuItem(uin.name, uin.mnemonic);
        tblRowsMICopy.addActionListener(evt -> execCommand(copyCmdToClipboard()));
        tblRowsPopupMenu.add(tblRowsMICopy);
        // sets the popup menu for the table
        tblCommands.setComponentPopupMenu(tblRowsPopupMenu);
        tblCommands.setOpaque(true);
        tblCommands.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    tblCommands.clearSelection();
                    int row = tblCommands.rowAtPoint(e.getPoint());
                    tblCommands.addRowSelectionInterval(row, row);
                }
            }
        });

        btnFavs = new JButton[favBtnLimit];
        for (int i = 0; i < favBtnLimit; i++) {
            String s = "" + (i == 9 ? 0 : i + 1);
            JButton b = new AppButton(s, s.charAt(0));
            b.addActionListener(evt -> execCommand(b.getToolTipText()));
            btnFavs[i] = b;
        }
        redrawFavBtns();

        lblRecents = new JLabel[recentLblLimit];
        for (int i = 0; i < recentLblLimit; i++) {
            JLabel l = new JLabel();
            l.setHorizontalAlignment(JLabel.CENTER);
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        txtFilter.setText(l.getText());
                        updateRecentFilters();
                    }
                }
            });
            lblRecents[i] = l;
        }
        redrawRecentLbls();

        favBtnPanel = new JPanel(new GridLayout(favBtnLimit / BTN_IN_A_ROW, 1));
        JPanel favBtnPanel1 = new JPanel(new GridBagLayout());
        JPanel favBtnPanel2 = new JPanel(new GridBagLayout());
        favBtnPanel.add(favBtnPanel1);
        toColor.add(favBtnPanel);
        toColor.add(favBtnPanel1);
        toColor.add(favBtnPanel2);
        if (btnFavs.length > BTN_IN_A_ROW) {
            favBtnPanel.add(favBtnPanel2);
        }
        titledFP = new TitledBorder(FAV_HEADING);
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
        toColor.add(topPanel);
        topPanel.add(lblInfo);
        //topPanel.add(controlPanel);
        topPanel.add(favBtnPanel);
        topPanel.setBorder(EMPTY_BORDER);

        JPanel lowerPanel = new JPanel(new BorderLayout());
        toColor.add(lowerPanel);
        JScrollPane jspCmds = new JScrollPane(tblCommands);

        JPanel filterPanelParent = new JPanel(new GridLayout(2, 1));
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setLayout(new GridBagLayout());
        filterPanel.add(lblFilter);
        filterPanel.add(jtb);
        filterPanel.add(btnClear);
        filterPanel.add(btnExit);
        filterPanel.setBorder(EMPTY_BORDER);
        JPanel recentLblPanel = new JPanel(new GridLayout(1, recentLblLimit));
        Arrays.stream(lblRecents).forEach(recentLblPanel::add);
        filterPanelParent.add(filterPanel);
        filterPanelParent.add(recentLblPanel);

        lowerPanel.add(filterPanelParent, BorderLayout.NORTH);
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

        addBindings();
        setControlsToEnable();
        setPosition();

        new Timer().schedule(new ColorChangerTask(this), SEC_1, MIN_10);
        new Timer().schedule(new ThemeChangerTask(this), SEC_1, MIN_10);

        if (configs.getBooleanConfig(Configs.AutoLock.name())) {
            applyWindowActiveCheck(new WindowChecks[]{WindowChecks.WINDOW_ACTIVE, WindowChecks.AUTO_LOCK});
        } else {
            applyWindowActiveCheck(new WindowChecks[]{WindowChecks.WINDOW_ACTIVE});
        }

        if (!Boolean.parseBoolean(getRandomThemes())) {
            changeTheme();
        }
        if (!Boolean.parseBoolean(getRandomColors())) {
            changeColor();
        }

        txtFilter.setText(getCfg(Configs.Filter));
        txtFilter.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateRecentFilters();
            }
        });
    }

    private String[] getRecentFiltersList() {
        // remove empty
        return Arrays.stream(recentFiltersStr.split(SEPARATOR)).filter(Utils::hasValue).toArray(String[]::new);
    }

    private String copyCmdToClipboard() {
        return ".\\cmds\\cp-clip.bat \"" + commandUtil.getCmdToRun(getSelectedRowText(tblCommands, 0)) + "\"";
    }

    public void debug(String s) {
        logger.debug(s);
    }

    private void updateRecentFilters() {
        logger.info("update recent filter values");

        String s = getFilter();
        recentFiltersStr = checkItems(s, recentFiltersStr);
        String[] arrF = getRecentFiltersList();
        updateRecentMenu(menuRFilters, arrF, txtFilter, TXT_F_MAP_KEY);
        txtFilter.setAutoCompleteArr(arrF);
        redrawRecentLbls();
    }

    private String checkItems(String searchStr, String csv) {
        if (!Utils.hasValue(searchStr)) {
            return csv;
        }

        String csvLC = csv.toLowerCase();
        String ssp = SEPARATOR + searchStr;
        String ss = ssp + SEPARATOR;
        String ssLC = ss.toLowerCase();
        if (csvLC.contains(ssLC)) {
            int idx = csvLC.indexOf(ssLC);
            // remove item and add it again to bring it on top
            csv = csv.substring(0, idx)
                    + SEPARATOR + csv.substring(idx + ssLC.length());
        }
        csv = ssp + csv;

        if (csv.split(SEPARATOR).length > RECENT_LIMIT) {
            csv = csv.substring(0, csv.lastIndexOf(SEPARATOR) + SEPARATOR.length());
        }

        return csv;
    }

    private String[] getFilters() {
        return getCfg(Configs.RecentFilters).split(SEPARATOR);
    }

    public String getCfg(Configs c) {
        return configs.getConfig(c.name());
    }

    public String getAppProp(AppProps a) {
        return appProps.getProperty(a.name());
    }

    private void createAppMenu() {
        mbarSettings = new JMenuBar();
        JMenu menuSettings = new JMenu("Settings");
        menuTime = new JMenu("");
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

        jcbmiApplyToApp = new JCheckBoxMenuItem("Apply color to App", null, configs.getBooleanConfig(Configs.ApplyColorToApp.name()));
        jcbmiApplyToApp.setMnemonic('y');
        jcbmiApplyToApp.setToolTipText("Changes colors of complete application whenever highlight color changes");

        jcbmiAutoLock = new JCheckBoxMenuItem("Auto Lock", null, configs.getBooleanConfig(Configs.AutoLock.name()));
        jcbmiAutoLock.setMnemonic('L');
        jcbmiAutoLock.setToolTipText("Auto Lock App if idle for 10 min - change need restart");

        jcbmiShowFullCmd = new JCheckBoxMenuItem("Show full command", null, showFullCmd);
        jcbmiShowFullCmd.setMnemonic('W');
        jcbmiShowFullCmd.setToolTipText("Show full command");
        jcbmiShowFullCmd.addActionListener(evt -> {
            showFullCmd = jcbmiShowFullCmd.isSelected();
            reloadFile();
        });
        menuSettings.add(jcbRC);
        menuSettings.add(SwingUtils.getColorsMenu(true, true,
                false, true, false, this, logger));
        menuSettings.addSeparator();
        menuSettings.add(jcbRT);
        menuSettings.add(SwingUtils.getThemesMenu(this, logger));
        menuSettings.addSeparator();

        UIName u = UIName.MNU_CFG;
        JMenuItem miCfg = new JMenuItem(u.name);
        miCfg.setMnemonic(u.mnemonic);
        miCfg.setToolTipText(u.tip);
        miCfg.addActionListener(e -> openConfig());
        menuSettings.add(miCfg);

        u = UIName.MNU_CMD;
        JMenuItem miCmd = new JMenuItem(u.name);
        miCmd.setMnemonic(u.mnemonic);
        miCmd.setToolTipText(u.tip);
        miCmd.addActionListener(e -> openCmd());
        menuSettings.add(miCmd);

        menuSettings.addSeparator();
        u = UIName.MNU_CLOSE_15;
        JMenuItem mi15 = new JMenuItem(u.name);
        mi15.setMnemonic(u.mnemonic);
        mi15.setToolTipText(u.tip);
        mi15.addActionListener(e -> runTimerCmd(closeCommandStr, MIN_15));
        menuSettings.add(mi15);

        u = UIName.MNU_CLOSE_30;
        JMenuItem mi30 = new JMenuItem(u.name);
        mi30.setMnemonic(u.mnemonic);
        mi30.setToolTipText(u.tip);
        mi30.addActionListener(e -> runTimerCmd(closeCommandStr, MIN_30));
        menuSettings.add(mi30);

        u = UIName.MNU_TIMER_CANCEL;
        JMenuItem miCancel = new JMenuItem(u.name);
        miCancel.setMnemonic(u.mnemonic);
        miCancel.setToolTipText(u.tip);
        miCancel.addActionListener(e -> cancelTrackTimer());
        menuSettings.add(miCancel);

        menuSettings.addSeparator();
        menuSettings.add(jcbmiApplyToApp);
        menuSettings.addSeparator();
        JMenuItem jmiChangePwd = new JMenuItem("Change Password", 'c');
        jmiChangePwd.addActionListener(e -> showChangePwdScreen(highlightColor));
        JMenuItem jmiLock = new JMenuItem("Lock screen", 'o');
        jmiLock.addActionListener(e -> showLockScreen(highlightColor));
        menuSettings.add(jmiChangePwd);
        menuSettings.add(jmiLock);
        menuSettings.add(jcbmiAutoLock);
        menuSettings.addSeparator();
        menuSettings.add(jcbmiShowFullCmd);

        mbarSettings.add(menuSettings);
        mbarSettings.add(menuTime);

        setJMenuBar(mbarSettings);
    }

    @Override
    public void pwdChangedStatus(boolean pwdChanged) {
        if (pwdChanged) {
            lastCmdRun = "Password changed";
            updateInfo();
        }
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

    private void addBindings() {

        Action actionGoToFirstRow = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                selectFirstRow ();
            }
        };
        Action actionGoToFilter = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SwingUtils.getInFocus (txtFilter);
            }
        };

        List<KeyActionDetails> keyActionDetails = new ArrayList<>();
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_KP_DOWN, KeyEvent.ALT_DOWN_MASK, actionGoToFirstRow));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK, actionGoToFirstRow));
        keyActionDetails.add(new KeyActionDetails(KS_CTRL_F, actionGoToFilter));

        final JComponent[] addBindingsTo = {txtFilter, mb, btnReload, btnChangePwd, btnLock, btnClear};
        SwingUtils.addKeyBindings(addBindingsTo, keyActionDetails);
    }

    private void selectFirstRow() {
        SwingUtils.getInFocus(tblCommands);
        if (tblCommands.getRowCount() > 0) {
            tblCommands.changeSelection(0, 0, false, false);
        }
    }

    private void applyColor(ColorsNFonts color) {
        if (isWindowActive()) {
            //logger.info("Applying color: " + color.name().toLowerCase());
            highlightColor = color.getBk();
            highlightTextColor = color.getFg();
            selectionColor = color.getSelbk();
            selectionTextColor = color.getSelfg();

            // setting color for lock screen for auto-lock feature
            setLockScreenColor(highlightColor);

            lblInfo.setBackground(highlightColor);
            lblInfo.setForeground(highlightTextColor);
            Font font = getLblInfoFont(color.getFont());
            logger.info("Applying color and font: " + font.getName());
            lblInfo.setFont(getLblInfoFont(color.getFont()));
            lblInfo.setBorder(new LineBorder(highlightTextColor, 3, true));
            lastColorApplied = color.name().toLowerCase();
            changeAppColor();
            updateInfo();
        }
    }

    private void changeAppColor() {
        Color cl = jcbmiApplyToApp.getState() ? highlightColor : ORIG_COLOR;

        titledFP = (TitledBorder) SwingUtils.createTitledBorder(FAV_HEADING, highlightTextColor);
        titledFP.setTitleColor(highlightTextColor);
        favBtnPanel.setBorder(titledFP);
        lblFilter.setForeground(selectionColor);
        mb.setBorder(SwingUtils.createLineBorder(selectionColor));
        tblCommands.setBorder(SwingUtils.createLineBorder(highlightTextColor));
        //tblCommands.getTableHeader().setBackground(highlightColor);
        JComponent[] ca = {btnClear, btnReload, btnLock, btnChangePwd, menuRFilters};
        SwingUtils.setComponentColor(btnFavs, cl, highlightTextColor, selectionColor, selectionTextColor);
        SwingUtils.setComponentColor(ca, cl, highlightTextColor, selectionColor, selectionTextColor);
        SwingUtils.setComponentColor(lblRecents, null, selectionColor, null, highlightColor);
        SwingUtils.setComponentColor(toColor.toArray(new JComponent[0]), cl, highlightTextColor);
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

    public void cancelTrackTimer() {
        logger.info("Cancelling command timer if running");
        cmdTimer.cancel();
        cmdTimerTrack.cancel();
        runCommandTimer = null;
        trackTimer();
    }

    public void trackTimer() {
        timerTrack = "";
        long sec = 0;
        if (runCommandTimer != null) {
            sec = runCommandTimer.getDateTimeDiffSec();
            timerTrack = runCommandTimer.getDateTimeDiff(sec);
        }
        menuTime.setText(timerTrack);
        if (sec < TimeUnit.MILLISECONDS.toSeconds(MIN_1)) {
            menuTime.setForeground(Color.RED);
        } else {
            menuTime.setForeground(Color.BLACK);
        }
        if (timerTrack.equals("0:00")) {
            cancelTrackTimer();
        }
    }

    public void runTimerCmd(String cmd, long time) {
        cancelTrackTimer();
        logger.info("Scheduling command " + Utils.addBraces(cmd) + " for time " + Utils.addBraces(Utils.getTimeMS(time)));
        runCommandTimer = new RunCommandTimer(logger, this, cmd, time);
        cmdTimerTrack = new Timer();
        cmdTimerTrack.schedule(new TimerTrackTask(this), 0, SEC_1);
        cmdTimer = new Timer();
        cmdTimer.schedule(runCommandTimer, time);
    }

    private void logThemeChangeInfo(UIManager.LookAndFeelInfo lfClass) {
        lastThemeApplied = lfClass.getName();
        updateInfo();
    }

    private void applyTheme(UIManager.LookAndFeelInfo lfClass) {
        if (isWindowActive()) {
            SwingUtils.applyTheme(themeIdx, lfClass, this, logger);
            SwingUtils.updateForTheme(tblCommands);
            //logThemeChangeInfo (lfClass);
        }
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
        redrawRecentLbls();
        enableControls();
    }

    private void openConfig() {
        execCommand("cmd.exe /c start .");
    }

    private void openCmd() {
        execCommand("cmd.exe /c start cmd.exe");
    }

    private void cleanFavBtns() {
        for (JButton b : btnFavs) {
            b.setText("X");
            b.setToolTipText("");
            b.setEnabled(false);
        }
    }

    private void updateRecentMenu(JMenu m, String[] arr, JTextField txtF, String mapKey) {
        m.removeAll();

        int i = 'a';
        for (String a : arr) {
            char ch = (char) i;
            JMenuItem mi = new JMenuItem(ch + SP_DASH_SP + a);
            mi.addActionListener(e -> txtF.setText(a));
            if (i <= 'z') {
                mi.setMnemonic(i++);
                addActionOnMenu(new RecentMenuAction(txtF, a), mi, ch, mapKey + ch);
                m.add(mi);
            }
        }
    }

    private void addActionOnMenu(AbstractAction action, JMenuItem mi, char keycode, String mapKey) {
        InputMap im = mi.getInputMap();
        im.put(KeyStroke.getKeyStroke(keycode, 0), mapKey);
        ActionMap am = mi.getActionMap();
        am.put(mapKey, action);
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

    private void redrawRecentLbls() {
        String[] recentList = getRecentFiltersList();
        int listLen = recentList.length;
        for (int i = 0; i < recentLblLimit; i++) {
            lblRecents[i].setText((i < listLen) ? recentList[i] : "");
            lblRecents[i].setToolTipText((i < listLen) ? "Click to apply filter" : "");
        }
    }

    // This will be called by reflection from SwingUI jar
    public void handleDblClickOnRow(AppTable table, Object[] p) {
        execCommand(getSelectedRowText(table, 0));
    }

    public String getSelectedRowText(AppTable table, int col) {
        return table.getValueAt(table.getSelectedRow(), col).toString();
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
        tblCommands.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCommands.setTableHeader(new TableHeaderToolTip(tblCommands.getColumnModel(),
                Arrays.stream(COLS.class.getEnumConstants()).map(COLS::getToolTip).toArray(String[]::new)
        ));
        tblCommands.setBorder(borderBlue);
        // due to theme it gets override
        //toColor.add(tblCommands.getTableHeader());

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

        String txt = HTML_STR + CENTER_STR + cmdRun + SP_DASH_SP
                + getAppProp(AppProps.Host) + BR + BR +
                "<span style='font-size:9px'>" + tip2 + SPAN_END + CENTER_END + HTML_END;
        lblInfo.setText(txt);

        //logger.info(tip + ", Thread pool current size: " + threadPool.toString());
        logger.info(tip);
    }

    public void runCmdCallable(String cmd) {
        String msg = commandUtil.execCommand(cmd);
        logger.info("Message from running command " + Utils.addBraces(msg));
        if (!Utils.hasValue(msg) || msg.equalsIgnoreCase("true")) {
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
        List<String> commands = Utils.readFile("./commands.config", logger);

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
                tblCommands.setValueAt(showFullCmd ? command : getDisplayName(command), i, COLS.COMMAND.getIdx());
            } else {
                model.addRow(new String[]{command, showFullCmd ? command : getDisplayName(command)});
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
        cancelTrackTimer();
        setVisible(false);
        dispose();
        logger.dispose();
        System.exit(0);
    }

    public String getRecentFilters() {
        return recentFiltersStr;
    }

    public String getCloseCommand() {
        return closeCommandStr;
    }

    public String getFilter() {
        return txtFilter.getText();
    }

    public String getRandomThemes() {
        return jcbRT.isSelected() + "";
    }

    public String getRandomColors() {
        return jcbRC.isSelected() + "";
    }

    public String getApplyColorToApp() {
        return jcbmiApplyToApp.isSelected() + "";
    }

    public String getAutoLock() {
        return jcbmiAutoLock.isSelected() + "";
    }

    public String getShowFullCmd() {
        return showFullCmd + "";
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

