package ui;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class InspectorPanel extends JPanel {
    private final DefaultTableModel headersModel;
    private final DefaultTableModel paramsModel;
    private final DefaultTableModel cookiesModel;
    private final JTabbedPane tabbedPane;

    public InspectorPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Inspector"));

        tabbedPane = new JTabbedPane();

        // Headers Table
        headersModel = new DefaultTableModel(new Object[] { "Name", "Value" }, 0);
        JTable headersTable = createTable(headersModel);
        tabbedPane.addTab("Headers", new JScrollPane(headersTable));

        // Params Table
        paramsModel = new DefaultTableModel(new Object[] { "Type", "Name", "Value" }, 0);
        JTable paramsTable = createTable(paramsModel);
        tabbedPane.addTab("Params", new JScrollPane(paramsTable));

        // Cookies Table
        cookiesModel = new DefaultTableModel(new Object[] { "Name", "Value" }, 0);
        JTable cookiesTable = createTable(cookiesModel);
        tabbedPane.addTab("Cookies", new JScrollPane(cookiesTable));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        return table;
    }

    public void setRequest(HttpRequest request) {
        clearModels();
        if (request == null)
            return;

        // Headers
        for (HttpHeader header : request.headers()) {
            headersModel.addRow(new Object[] { header.name(), header.value() });
        }

        // Params and Cookies
        for (ParsedHttpParameter param : request.parameters()) {
            if (param.type() == burp.api.montoya.http.message.params.HttpParameterType.COOKIE) {
                cookiesModel.addRow(new Object[] { param.name(), param.value() });
            } else {
                paramsModel.addRow(new Object[] { param.type().name(), param.name(), param.value() });
            }
        }
    }

    // For responses, we mainly have headers and maybe cookies (Set-Cookie)
    public void setResponse(HttpResponse response) {
        // Reuse same tables? Or maybe Inspector should differentiate?
        // Typically Inspector shows Request OR Response data depending on what is
        // viewed.
        // For simplicity, let's just show headers/cookies (Set-Cookie) for response.
        // But params don't exist for response.

        clearModels();
        if (response == null)
            return;

        for (HttpHeader header : response.headers()) {
            headersModel.addRow(new Object[] { header.name(), header.value() });
        }

        // We could parse Set-Cookie headers manually if we wanted,
        // but Montoya HttpResponse doesn't parse cookies automatically like Request.
        // So we leave cookies/params empty for response, or just show headers.
    }

    private void clearModels() {
        headersModel.setRowCount(0);
        paramsModel.setRowCount(0);
        cookiesModel.setRowCount(0);
    }
}
