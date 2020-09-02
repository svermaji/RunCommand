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
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

public class RunCommand extends AppFrame {

    private static final int DEFAULT_NUM_ROWS = 10;

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

    private MyLogger logger;
    private DefaultConfigs configs;
    private DefaultTableModel model;
    private JTable tblCommands;

    private JTextField txtFilter;
    private JButton btnReload, btnExit;

    private TableRowSorter<DefaultTableModel> sorter;

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(3);

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

        JPanel inputPanel = new JPanel();

        setTitle("Run Utility");

        Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

        final int TXT_COLS = 15;
        JLabel lblFilter = new JLabel("Filter");
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

        btnExit = new AppExitButton();

        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(lblFilter);
        inputPanel.add(txtFilter);
        inputPanel.add(btnReload);
        inputPanel.add(btnExit);
        inputPanel.setBorder(emptyBorder);

        createTable();

        JScrollPane jspCmds = new JScrollPane(tblCommands);
        jspCmds.setBorder(emptyBorder);

        parentContainer.add(inputPanel, BorderLayout.NORTH);
        parentContainer.add(jspCmds, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        setToCenter();
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
        threadPool.submit(new UtilityCallable(this, cmd));
    }

    private void clearOldRun() {
        //empty table
        IntStream.range(0, tblCommands.getRowCount()).forEach(i -> model.removeRow(0));
        createDefaultRows();
    }

    private void createRows() {
        List<String> commands = readCommands();

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

    private void setToCenter() {
        setState(JFrame.MAXIMIZED_BOTH);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Exit the Application
     */
    private void exitForm() {
        setVisible(false);
        dispose();
        logger.dispose();
        System.exit(0);
    }

    static class UtilityCallable implements Callable<Boolean> {

        private final RunCommand ru;
        private final String cmd;

        public UtilityCallable(RunCommand rc, String command) {
            this.ru = rc;
            this.cmd = command;
        }

        @Override
        public Boolean call() {
            try {
                String cmdStr = cmd.contains(" (") ? cmd.substring(0, cmd.indexOf(" (")) : cmd;
                ru.logger.log("Calling command [" + cmdStr + "]");
                Runtime.getRuntime().exec(cmdStr);
            } catch (Exception e) {
                ru.logger.error(e);
            }
            return true;
        }
    }

    class RunCommandAction extends AbstractAction {

        private JTable table;

        public RunCommandAction(JTable table) {
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            runCommand(table.getValueAt(row, 0).toString());
        }
    }
}
