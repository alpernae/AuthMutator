import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import handler.RequestHandler;
import model.ExtensionConfig;
import model.RequestLogModel;
import ui.MainPanel;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private RequestLogModel requestLogModel;
    private ExtensionConfig config;
    private RequestHandler requestHandler;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Auth Mutator");

        // Initialize the config
        config = new ExtensionConfig();

        // Initialize the data model
        requestLogModel = new RequestLogModel();

        // Register HTTP handler
        requestHandler = new RequestHandler(api, requestLogModel, config);
        api.http().registerHttpHandler(requestHandler);

        // Create and register the UI
        MainPanel mainPanel = new MainPanel(api, requestLogModel, config, requestHandler);
        api.userInterface().registerSuiteTab("Auth Mutator", mainPanel);

        api.logging().logToOutput("Auth Mutator Loaded Successfully!");
        api.logging().logToOutput("Author: ALPEREN ERGEL (@alpernae)");
        api.logging().logToOutput("Version: 0.1.0.");

        api.extension().registerUnloadingHandler(() -> {
            api.logging().logToOutput("Auth Mutator Unload");
            if (requestHandler != null) {
                requestHandler.shutdown();
            }
        });
    }

    public RequestHandler getRequestHandler() {
        return requestHandler;
    }
}