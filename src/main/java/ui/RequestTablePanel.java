package ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import model.RequestLogEntry;
import model.RequestLogModel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import static javax.swing.SwingUtilities.invokeLater;

public class RequestTablePanel extends JPanel {
    private final MontoyaApi api;
    private final RequestLogModel requestLogModel;
    private final JTable requestTable;
    private final TableRowSorter<RequestLogModel> sorter;
    private java.util.List<model.HighlightRule> highlightRules;
    private HttpRequestEditor originalRequestEditor;
    private HttpRequestEditor modifiedRequestEditor;
    private HttpRequestEditor unauthRequestEditor;
    private HttpResponseEditor originalResponseEditor;
    private HttpResponseEditor modifiedResponseEditor;
    private HttpResponseEditor unauthResponseEditor;
    private JTabbedPane requestTabbedPane;
    private JTabbedPane responseTabbedPane;
    private JTextPane requestDiffPane;
    private JTextPane responseDiffPane;
    private int requestUnauthTabIndex;
    private int requestDiffTabIndex;
    private int responseUnauthTabIndex;
    private int responseDiffTabIndex;

    public RequestTablePanel(MontoyaApi api, RequestLogModel requestLogModel) {
        this.api = api;
        this.requestLogModel = requestLogModel;
        this.highlightRules = new java.util.ArrayList<>();

        setLayout(new BorderLayout());

        // Create table with model
        requestTable = new JTable(requestLogModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    int modelRow = convertRowIndexToModel(row);
                    model.RequestLogEntry entry = requestLogModel.getEntry(modelRow);
                    Color highlightColor = getHighlightColor(entry);
                    Color modifiedColor = (highlightColor == null && entry != null && entry.wasModified())
                            ? new Color(255, 255, 200)
                            : null;
                    if (highlightColor != null) {
                        c.setBackground(highlightColor);
                        c.setForeground(getContrastColor(highlightColor));
                    } else if (modifiedColor != null) {
                        c.setBackground(modifiedColor);
                        c.setForeground(getContrastColor(modifiedColor));
                    } else {
                        c.setBackground(getBackground());
                        c.setForeground(getForeground());
                    }
                }
                return c;
            }
        };
        sorter = new TableRowSorter<>(requestLogModel);
        requestTable.setRowSorter(sorter);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        requestLogModel.addTableModelListener(e -> {
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                invokeLater(this::refreshColumnLayout);
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(requestTable);

        // Create request/response viewer
        JSplitPane messageViewerPane = createMessageViewer();

        // Split pane for table and message viewer
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, messageViewerPane);
        mainSplitPane.setDividerLocation(300);

        add(mainSplitPane, BorderLayout.CENTER);

        // Add selection listener
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displaySelectedRequest();
            }
        });

        refreshColumnLayout();
    }

    private JSplitPane createMessageViewer() {
        // Create request editors
        originalRequestEditor = api.userInterface().createHttpRequestEditor();
    modifiedRequestEditor = api.userInterface().createHttpRequestEditor();
    unauthRequestEditor = api.userInterface().createHttpRequestEditor();

        // Create response editors
        originalResponseEditor = api.userInterface().createHttpResponseEditor();
        modifiedResponseEditor = api.userInterface().createHttpResponseEditor();
    unauthResponseEditor = api.userInterface().createHttpResponseEditor();

        // Request tabbed pane
        requestTabbedPane = new JTabbedPane();

        JPanel originalReqPanel = new JPanel(new BorderLayout());
        originalReqPanel.add(originalRequestEditor.uiComponent(), BorderLayout.CENTER);
        requestTabbedPane.addTab("Original Request", originalReqPanel);

        JPanel modifiedReqPanel = new JPanel(new BorderLayout());
        modifiedReqPanel.add(modifiedRequestEditor.uiComponent(), BorderLayout.CENTER);
        requestTabbedPane.addTab("Modified Request", modifiedReqPanel);

        JPanel unauthReqPanel = new JPanel(new BorderLayout());
        unauthReqPanel.add(unauthRequestEditor.uiComponent(), BorderLayout.CENTER);
        requestUnauthTabIndex = requestTabbedPane.getTabCount();
        requestTabbedPane.addTab("Unauth Request", unauthReqPanel);
        requestTabbedPane.setEnabledAt(requestUnauthTabIndex, false);

    requestDiffPane = createDiffPane();
    JScrollPane requestDiffScroll = new JScrollPane(requestDiffPane);
        requestDiffTabIndex = requestTabbedPane.getTabCount();
        requestTabbedPane.addTab("Diff", requestDiffScroll);
        requestTabbedPane.setEnabledAt(requestDiffTabIndex, false);

        JPanel requestContainer = new JPanel(new BorderLayout());
        JLabel reqLabel = new JLabel("Request");
        reqLabel.setFont(reqLabel.getFont().deriveFont(Font.BOLD));
        reqLabel.setForeground(PrimaryButton.PRIMARY_COLOR);
        reqLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        requestContainer.add(reqLabel, BorderLayout.NORTH);
        requestContainer.add(requestTabbedPane, BorderLayout.CENTER);

        // Response tabbed pane
        responseTabbedPane = new JTabbedPane();

        JPanel originalRespPanel = new JPanel(new BorderLayout());
        originalRespPanel.add(originalResponseEditor.uiComponent(), BorderLayout.CENTER);
        responseTabbedPane.addTab("Original Response", originalRespPanel);

        JPanel modifiedRespPanel = new JPanel(new BorderLayout());
        modifiedRespPanel.add(modifiedResponseEditor.uiComponent(), BorderLayout.CENTER);
        responseTabbedPane.addTab("Modified Response", modifiedRespPanel);

        JPanel unauthRespPanel = new JPanel(new BorderLayout());
        unauthRespPanel.add(unauthResponseEditor.uiComponent(), BorderLayout.CENTER);
        responseUnauthTabIndex = responseTabbedPane.getTabCount();
        responseTabbedPane.addTab("Unauth Response", unauthRespPanel);
        responseTabbedPane.setEnabledAt(responseUnauthTabIndex, false);

    responseDiffPane = createDiffPane();
    JScrollPane responseDiffScroll = new JScrollPane(responseDiffPane);
        responseDiffTabIndex = responseTabbedPane.getTabCount();
        responseTabbedPane.addTab("Diff", responseDiffScroll);
        responseTabbedPane.setEnabledAt(responseDiffTabIndex, false);

        JPanel responseContainer = new JPanel(new BorderLayout());
        JLabel respLabel = new JLabel("Response");
        respLabel.setFont(respLabel.getFont().deriveFont(Font.BOLD));
        respLabel.setForeground(PrimaryButton.PRIMARY_COLOR);
        respLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        responseContainer.add(respLabel, BorderLayout.NORTH);
        responseContainer.add(responseTabbedPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestContainer, responseContainer);
        splitPane.setDividerLocation(500);

        return splitPane;
    }

    public void refreshColumnLayout() {
        invokeLater(() -> {
            int columnCount = requestTable.getColumnModel().getColumnCount();
            if (columnCount == 0) {
                return;
            }

            int[] widths = {50, 80, 400, 80, 110, 100, 100};
            for (int i = 0; i < Math.min(widths.length, columnCount); i++) {
                requestTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }

            if (columnCount > 7) {
                requestTable.getColumnModel().getColumn(7).setPreferredWidth(70);
                DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
                requestTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
            }

            DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
            leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);
            for (int columnIndex : new int[]{0, 3, 4}) {
                if (columnIndex < columnCount) {
                    requestTable.getColumnModel().getColumn(columnIndex).setCellRenderer(leftRenderer);
                }
            }
        });
    }

    private void displaySelectedRequest() {
        int selectedRow = requestTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = requestTable.convertRowIndexToModel(selectedRow);
            RequestLogEntry entry = requestLogModel.getEntry(modelRow);
            
            if (entry != null) {
                requestTabbedPane.setSelectedIndex(0);
                responseTabbedPane.setSelectedIndex(0);

                HttpRequest originalRequest = entry.getOriginalRequest();
                HttpRequest modifiedRequest = entry.getModifiedRequest();
                HttpRequest unauthRequest = entry.getUnauthRequest();

                originalRequestEditor.setRequest(originalRequest);

                if (modifiedRequest != null) {
                    modifiedRequestEditor.setRequest(modifiedRequest);
                    requestTabbedPane.setEnabledAt(1, true);
                    DiffResult requestDiff = buildDiff(originalRequest, modifiedRequest);
                    renderDiff(requestDiffPane, requestDiff);
                    requestTabbedPane.setEnabledAt(requestDiffTabIndex, requestDiff.hasChanges());
                    if (!requestDiff.hasChanges() && requestTabbedPane.getSelectedIndex() == requestDiffTabIndex) {
                        requestTabbedPane.setSelectedIndex(0);
                    }
                } else {
                    modifiedRequestEditor.setRequest(originalRequest);
                    requestTabbedPane.setEnabledAt(1, false);
                    renderDiff(requestDiffPane, DiffResult.info("No modified request available."));
                    requestTabbedPane.setEnabledAt(requestDiffTabIndex, false);
                    if (requestTabbedPane.getSelectedIndex() == requestDiffTabIndex) {
                        requestTabbedPane.setSelectedIndex(0);
                    }
                }

                if (unauthRequest != null) {
                    unauthRequestEditor.setRequest(unauthRequest);
                    requestTabbedPane.setEnabledAt(requestUnauthTabIndex, true);
                } else {
                    unauthRequestEditor.setRequest(originalRequest);
                    requestTabbedPane.setEnabledAt(requestUnauthTabIndex, false);
                    if (requestTabbedPane.getSelectedIndex() == requestUnauthTabIndex) {
                        requestTabbedPane.setSelectedIndex(0);
                    }
                }

                HttpResponse originalResponse = entry.getOriginalResponse();
                HttpResponse modifiedResponse = entry.getModifiedResponse();
                HttpResponse unauthResponse = entry.getUnauthResponse();

                if (originalResponse != null) {
                    originalResponseEditor.setResponse(originalResponse);
                } else if (entry.getResponse() != null) {
                    originalResponseEditor.setResponse(entry.getResponse());
                }

                if (modifiedResponse != null) {
                    modifiedResponseEditor.setResponse(modifiedResponse);
                    responseTabbedPane.setEnabledAt(1, true);
                } else {
                    if (entry.getResponse() != null) {
                        modifiedResponseEditor.setResponse(entry.getResponse());
                    }
                    responseTabbedPane.setEnabledAt(1, false);
                }

                if (originalResponse != null && modifiedResponse != null) {
                    DiffResult responseDiff = buildDiff(originalResponse, modifiedResponse);
                    renderDiff(responseDiffPane, responseDiff);
                    responseTabbedPane.setEnabledAt(responseDiffTabIndex, responseDiff.hasChanges());
                    if (!responseDiff.hasChanges() && responseTabbedPane.getSelectedIndex() == responseDiffTabIndex) {
                        responseTabbedPane.setSelectedIndex(0);
                    }
                } else {
                    renderDiff(responseDiffPane, DiffResult.info("No diff available."));
                    responseTabbedPane.setEnabledAt(responseDiffTabIndex, false);
                    if (responseTabbedPane.getSelectedIndex() == responseDiffTabIndex) {
                        responseTabbedPane.setSelectedIndex(0);
                    }
                }

                if (unauthResponse != null) {
                    unauthResponseEditor.setResponse(unauthResponse);
                    responseTabbedPane.setEnabledAt(responseUnauthTabIndex, true);
                } else {
                    unauthResponseEditor.setResponse(null);
                    responseTabbedPane.setEnabledAt(responseUnauthTabIndex, false);
                    if (responseTabbedPane.getSelectedIndex() == responseUnauthTabIndex) {
                        responseTabbedPane.setSelectedIndex(0);
                    }
                }
            }
        }
    }

    public void applyFilter(RowFilter<RequestLogModel, Integer> filter) {
        sorter.setRowFilter(filter);
    }

    public void clearFilter() {
        sorter.setRowFilter(null);
    }

    public void setHighlightRules(java.util.List<model.HighlightRule> rules) {
        this.highlightRules = rules;
        requestTable.repaint();
    }

    private Color getContrastColor(Color background) {
        if (background == null) {
            return Color.BLACK;
        }
        double luminance = (0.2126 * background.getRed()) + (0.7152 * background.getGreen()) + (0.0722 * background.getBlue());
        return luminance < 140 ? Color.WHITE : Color.BLACK;
    }

    private Color getHighlightColor(model.RequestLogEntry entry) {
        if (entry == null || highlightRules == null) {
            return null;
        }
        for (model.HighlightRule rule : highlightRules) {
            try {
                if (rule.matches(entry)) {
                    return rule.getColor();
                }
            } catch (Exception ignored) {
                // Rule evaluation failed; skip to the next rule.
            }
        }
        return null;
    }

    private JTextPane createDiffPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        pane.setMargin(new Insets(6, 8, 6, 8));
        Color background = UIManager.getColor("TextArea.background");
        if (background == null) {
            background = UIManager.getColor("Panel.background");
        }
        if (background == null) {
            background = Color.WHITE;
        }
        Color foreground = UIManager.getColor("TextArea.foreground");
        if (foreground == null) {
            foreground = UIManager.getColor("Panel.foreground");
        }
        if (foreground == null) {
            foreground = Color.DARK_GRAY;
        }
        pane.setBackground(background);
        pane.setForeground(foreground);
        return pane;
    }

    private void renderDiff(JTextPane pane, DiffResult diff) {
        ensureDiffStyles(pane);
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            for (DiffLine line : diff.lines()) {
                Style style = pane.getStyle(line.type().name());
                if (style == null) {
                    style = pane.getStyle(DiffType.INFO.name());
                }
                doc.insertString(doc.getLength(), line.text() + '\n', style);
            }
        } catch (BadLocationException ignored) {
            StringBuilder fallback = new StringBuilder();
            for (DiffLine line : diff.lines()) {
                fallback.append(line.text()).append('\n');
            }
            pane.setText(fallback.toString());
        }
        pane.setCaretPosition(0);
    }

    private DiffResult buildDiff(HttpRequest original, HttpRequest modified) {
        if (original == null || modified == null) {
            return DiffResult.info("No diff available.");
        }
        return buildDiff(original.toString(), modified.toString());
    }

    private DiffResult buildDiff(HttpResponse original, HttpResponse modified) {
        if (original == null || modified == null) {
            return DiffResult.info("No diff available.");
        }
        return buildDiff(original.toString(), modified.toString());
    }

    private DiffResult buildDiff(String original, String modified) {
        if (original == null) {
            original = "";
        }
        if (modified == null) {
            modified = "";
        }
        if (original.equals(modified)) {
            return DiffResult.info("No differences.");
        }

        String[] originalLines = original.split("\\R", -1);
        String[] modifiedLines = modified.split("\\R", -1);
        int[][] lcs = new int[originalLines.length + 1][modifiedLines.length + 1];

        for (int row = originalLines.length - 1; row >= 0; row--) {
            for (int col = modifiedLines.length - 1; col >= 0; col--) {
                if (originalLines[row].equals(modifiedLines[col])) {
                    lcs[row][col] = lcs[row + 1][col + 1] + 1;
                } else {
                    lcs[row][col] = Math.max(lcs[row + 1][col], lcs[row][col + 1]);
                }
            }
        }

        java.util.List<DiffLine> lines = new java.util.ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < originalLines.length && j < modifiedLines.length) {
            if (originalLines[i].equals(modifiedLines[j])) {
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                lines.add(new DiffLine(DiffType.REMOVED, "- " + originalLines[i]));
                i++;
            } else {
                lines.add(new DiffLine(DiffType.ADDED, "+ " + modifiedLines[j]));
                j++;
            }
        }
        while (i < originalLines.length) {
            lines.add(new DiffLine(DiffType.REMOVED, "- " + originalLines[i]));
            i++;
        }
        while (j < modifiedLines.length) {
            lines.add(new DiffLine(DiffType.ADDED, "+ " + modifiedLines[j]));
            j++;
        }

        if (lines.isEmpty()) {
            return DiffResult.info("No differences.");
        }

        return DiffResult.changes(lines);
    }

    private void ensureDiffStyles(JTextPane pane) {
        if (pane.getStyle(DiffType.INFO.name()) != null) {
            return;
        }

        Color baseBackground = pane.getBackground();
        if (baseBackground == null) {
            baseBackground = UIManager.getColor("TextArea.background");
        }
        if (baseBackground == null) {
            baseBackground = Color.WHITE;
        }

        Color baseForeground = pane.getForeground();
        if (baseForeground == null) {
            baseForeground = UIManager.getColor("TextArea.foreground");
        }
        if (baseForeground == null) {
            baseForeground = Color.BLACK;
        }

        Style info = pane.addStyle(DiffType.INFO.name(), null);
        StyleConstants.setBackground(info, baseBackground);
        StyleConstants.setForeground(info, baseForeground);
        StyleConstants.setFontFamily(info, pane.getFont().getFamily());
        StyleConstants.setFontSize(info, pane.getFont().getSize());

        Style added = pane.addStyle(DiffType.ADDED.name(), info);
        Color addedBg = blendColors(baseBackground, DIFF_ADDED_COLOR, 0.35f);
        StyleConstants.setBackground(added, addedBg);
        StyleConstants.setForeground(added, getContrastColor(addedBg));

        Style removed = pane.addStyle(DiffType.REMOVED.name(), info);
        Color removedBg = blendColors(baseBackground, DIFF_REMOVED_COLOR, 0.35f);
        StyleConstants.setBackground(removed, removedBg);
        StyleConstants.setForeground(removed, getContrastColor(removedBg));
    }

    private static Color blendColors(Color base, Color target, float ratio) {
        ratio = Math.min(1f, Math.max(0f, ratio));
        float inverse = 1f - ratio;
        int r = Math.round((base.getRed() * inverse) + (target.getRed() * ratio));
        int g = Math.round((base.getGreen() * inverse) + (target.getGreen() * ratio));
        int b = Math.round((base.getBlue() * inverse) + (target.getBlue() * ratio));
        return new Color(r, g, b);
    }

    private enum DiffType {
        ADDED,
        REMOVED,
        INFO
    }

    private static class DiffLine {
        private final DiffType type;
        private final String text;

        DiffLine(DiffType type, String text) {
            this.type = type;
            this.text = text;
        }

        DiffType type() {
            return type;
        }

        String text() {
            return text;
        }
    }

    private static class DiffResult {
        private final java.util.List<DiffLine> lines;
        private final boolean hasChanges;

        private DiffResult(java.util.List<DiffLine> lines, boolean hasChanges) {
            this.lines = lines;
            this.hasChanges = hasChanges;
        }

        static DiffResult changes(java.util.List<DiffLine> lines) {
            return new DiffResult(java.util.List.copyOf(lines), true);
        }

        static DiffResult info(String message) {
            return new DiffResult(java.util.List.of(new DiffLine(DiffType.INFO, message)), false);
        }

        java.util.List<DiffLine> lines() {
            return lines;
        }

        boolean hasChanges() {
            return hasChanges;
        }
    }

    private static final Color DIFF_ADDED_COLOR = new Color(0x1E88E5);
    private static final Color DIFF_REMOVED_COLOR = new Color(0xE53935);
}
