package ui;

import model.RequestLogModel;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FilterPanel extends JPanel {
    private final RequestTablePanel requestTablePanel;

    private JComboBox<String> methodFilter;
    private JComboBox<String> statusFilter;
    private JTextField urlFilter;
    private JTextField idFilter;
    private JButton applyButton;
    private JButton clearButton;

    public FilterPanel(RequestLogModel requestLogModel, RequestTablePanel requestTablePanel) {
        this.requestTablePanel = requestTablePanel;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(BorderFactory.createTitledBorder("Filters"));

        // Method filter
        add(new JLabel("Method:"));
        methodFilter = new JComboBox<>(
                new String[] { "All", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD" });
        add(methodFilter);

        // Status code filter
        add(new JLabel("Status:"));
        statusFilter = new JComboBox<>(new String[] { "All", "2xx", "3xx", "4xx", "5xx" });
        add(statusFilter);

        // URL filter
        add(new JLabel("URL contains:"));
        urlFilter = new JTextField(20);
        add(urlFilter);

        // ID filter
        add(new JLabel("ID:"));
        idFilter = new JTextField(10);
        add(idFilter);

        // Buttons
        applyButton = new JButton("Apply Filters");
        applyButton.addActionListener(e -> applyFilters());
        add(applyButton);

        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearFilters());
        add(clearButton);
    }

    private void applyFilters() {
        List<RowFilter<RequestLogModel, Integer>> filters = new ArrayList<>();

        // Method filter
        String method = (String) methodFilter.getSelectedItem();
        if (method != null && !method.equals("All")) {
            filters.add(RowFilter.regexFilter(method, 1));
        }

        // Status filter
        String status = (String) statusFilter.getSelectedItem();
        if (status != null && !status.equals("All")) {
            String statusPattern = status.replace("xx", "\\d\\d");
            filters.add(RowFilter.regexFilter(statusPattern, 3));
        }

        // URL filter
        String url = urlFilter.getText().trim();
        if (!url.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + url, 2));
        }

        // ID filter
        String id = idFilter.getText().trim();
        if (!id.isEmpty()) {
            filters.add(RowFilter.regexFilter("^" + id + "$", 0));
        }

        // Apply combined filter
        if (!filters.isEmpty()) {
            RowFilter<RequestLogModel, Integer> combinedFilter = RowFilter.andFilter(filters);
            requestTablePanel.applyFilter(combinedFilter);
        } else {
            requestTablePanel.clearFilter();
        }
    }

    private void clearFilters() {
        methodFilter.setSelectedIndex(0);
        statusFilter.setSelectedIndex(0);
        urlFilter.setText("");
        idFilter.setText("");
        requestTablePanel.clearFilter();
    }
}
