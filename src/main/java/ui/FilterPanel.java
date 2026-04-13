package ui;

import model.RequestLogModel;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FilterPanel extends JPanel {
    private final RequestTablePanel requestTablePanel;

    private JComboBox<String> methodFilter;
    private JComboBox<String> statusFilter;
    private JTextField urlFilter;
    private JTextField idFilter;
    private JButton applyButton;
    private JButton clearButton;
    private Color defaultTextFieldBackground;

    public FilterPanel(RequestLogModel requestLogModel, RequestTablePanel requestTablePanel) {
        this.requestTablePanel = requestTablePanel;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Filters"),
                BorderFactory.createEmptyBorder(1, 4, 2, 4)));

        // Method filter
        add(new JLabel("Method:"));
        methodFilter = new JComboBox<>(
                new String[] { "All", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD" });
        methodFilter.setToolTipText("Filter by HTTP method");
        methodFilter.addActionListener(e -> applyFilters());
        add(methodFilter);

        // Status code filter
        add(new JLabel("Status:"));
        statusFilter = new JComboBox<>(new String[] { "All", "2xx", "3xx", "4xx", "5xx" });
        statusFilter.setToolTipText("Filter by original response status code");
        statusFilter.addActionListener(e -> applyFilters());
        add(statusFilter);

        // URL filter
        add(new JLabel("URL contains:"));
        urlFilter = new JTextField(20);
        urlFilter.setToolTipText("Case-insensitive URL text match");
        urlFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }
        });
        add(urlFilter);

        // ID filter
        add(new JLabel("ID:"));
        idFilter = new JTextField(10);
        idFilter.setToolTipText("Exact request ID");
        idFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }
        });
        defaultTextFieldBackground = idFilter.getBackground();
        add(idFilter);

        // Buttons
        applyButton = new PrimaryButton("Apply");
        applyButton.setToolTipText("Apply all active filters");
        applyButton.setMargin(new Insets(1, 8, 1, 8));
        applyButton.addActionListener(e -> applyFilters());
        add(applyButton);

        clearButton = new JButton("Reset");
        clearButton.setToolTipText("Clear all filters");
        clearButton.setMargin(new Insets(1, 8, 1, 8));
        clearButton.addActionListener(e -> clearFilters());
        add(clearButton);

        idFilter.addActionListener(e -> applyFilters());
        urlFilter.addActionListener(e -> applyFilters());
    }

    private void applyFilters() {
        List<RowFilter<RequestLogModel, Integer>> filters = new ArrayList<>();

        // Method filter
        String method = (String) methodFilter.getSelectedItem();
        if (method != null && !method.equals("All")) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(method) + "$", 2));
        }

        // Status filter
        String status = (String) statusFilter.getSelectedItem();
        if (status != null && !status.equals("All")) {
            int lowerBound = switch (status) {
                case "2xx" -> 200;
                case "3xx" -> 300;
                case "4xx" -> 400;
                case "5xx" -> 500;
                default -> -1;
            };
            if (lowerBound > 0) {
                int upperBound = lowerBound + 99;
                filters.add(new RowFilter<>() {
                    @Override
                    public boolean include(Entry<? extends RequestLogModel, ? extends Integer> entry) {
                        Object value = entry.getValue(4);
                        if (!(value instanceof Integer code)) {
                            return false;
                        }
                        return code >= lowerBound && code <= upperBound;
                    }
                });
            }
        }

        // URL filter
        String url = urlFilter.getText().trim();
        if (!url.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i).*" + Pattern.quote(url) + ".*", 3));
        }

        // ID filter
        String id = idFilter.getText().trim();
        if (!id.isEmpty()) {
            try {
                int idValue = Integer.parseInt(id);
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, idValue, 0));
                idFilter.setBackground(defaultTextFieldBackground);
            } catch (NumberFormatException ex) {
                idFilter.setBackground(new Color(255, 232, 232));
                return;
            }
        } else {
            idFilter.setBackground(defaultTextFieldBackground);
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
        idFilter.setBackground(defaultTextFieldBackground);
        requestTablePanel.clearFilter();
    }
}
