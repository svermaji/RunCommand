package com.sv.runcmd.helpers;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TableHeaderToolTip extends JTableHeader {

    private final String[] tips;

    public TableHeaderToolTip(TableColumnModel model, String[] tips) {
        super(model);
        this.tips = tips;
    }

    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int index = columnModel.getColumnIndexAtX(p.x);
        int realIndex = columnModel.getColumn(index).getModelIndex();
        return tips[realIndex];
    }
}