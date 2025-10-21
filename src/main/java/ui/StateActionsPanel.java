package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

class StateActionsPanel extends JPanel {
    StateActionsPanel(ActionListener importListener, ActionListener exportListener) {
        super(new FlowLayout(FlowLayout.RIGHT));
    JButton importButton = new PrimaryButton("Import All");
    importButton.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
    importButton.setMargin(new Insets(2, 10, 2, 10));
        importButton.addActionListener(importListener);
    JButton exportButton = new PrimaryButton("Export All");
    exportButton.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
    exportButton.setMargin(new Insets(2, 10, 2, 10));
        exportButton.addActionListener(exportListener);
        add(importButton);
        add(exportButton);
    }
}
