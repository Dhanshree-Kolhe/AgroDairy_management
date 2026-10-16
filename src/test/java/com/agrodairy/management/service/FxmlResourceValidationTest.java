package com.agrodairy.management.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FxmlResourceValidationTest {

    private final String[] fxmlPaths = new String[]{
            "/fxml/auth/LoginView.fxml",
            "/fxml/admin/AdminMainLayout.fxml",
            "/fxml/admin/AdminDashboardView.fxml",
            "/fxml/admin/AdminUsersView.fxml",
            "/fxml/admin/AdminProductsView.fxml",
            "/fxml/admin/AdminSystemStatusView.fxml",
            "/fxml/vendor/VendorMainLayout.fxml",
            "/fxml/vendor/VendorDashboardView.fxml",
            "/fxml/vendor/CustomerManagementView.fxml",
            "/fxml/vendor/DailyDeliveriesView.fxml",
            "/fxml/vendor/BillingView.fxml",
            "/fxml/vendor/InventoryView.fxml",
            "/fxml/vendor/ExtraOrdersView.fxml",
            "/fxml/vendor/HolidayRequestsView.fxml",
            "/fxml/vendor/RoutePlannerView.fxml",
            "/fxml/vendor/NotificationsView.fxml",
            "/fxml/customer/CustomerMainLayout.fxml",
            "/fxml/customer/CustomerDashboardView.fxml",
            "/fxml/customer/CustomerDeliveriesView.fxml",
            "/fxml/customer/CustomerBillsView.fxml",
            "/fxml/customer/CustomerHolidayView.fxml",
            "/fxml/customer/CustomerExtraOrdersView.fxml",
            "/fxml/customer/CustomerPaymentDialog.fxml",
            "/fxml/common/KnowledgeAssistantView.fxml"
    };

    @Test
    @DisplayName("Verify All FXML Files Exist on Classpath")
    public void testFxmlResourcesExist() {
        for (String path : fxmlPaths) {
            URL resource = getClass().getResource(path);
            assertNotNull(resource, "FXML resource must exist on classpath: " + path);
        }
    }

    @Test
    @DisplayName("Verify CSS Resource Exists on Classpath")
    public void testCssResourceExists() {
        URL cssResource = getClass().getResource("/styles/main.css");
        assertNotNull(cssResource, "Stylesheet main.css must exist on classpath");
    }

    @Test
    @DisplayName("Verify FXML Controllers and Action Handlers Exist via Reflection")
    public void testFxmlControllersAndHandlersExist() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        for (String path : fxmlPaths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                assertNotNull(is, "Cannot open stream for: " + path);
                Document doc = builder.parse(is);
                Element root = doc.getDocumentElement();

                String controllerClass = root.getAttributeNS("http://javafx.com/fxml", "controller");
                if (controllerClass.isEmpty()) {
                    controllerClass = root.getAttribute("fx:controller");
                }
                assertTrue(!controllerClass.isEmpty(), "FXML missing fx:controller: " + path);

                Class<?> clazz = Class.forName(controllerClass);
                assertNotNull(clazz, "Controller class not found: " + controllerClass);

                // Check all onAction handlers in the FXML
                Set<String> actionMethods = new HashSet<>();
                collectActionHandlers(root, actionMethods);

                for (String handlerName : actionMethods) {
                    boolean found = false;
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getName().equals(handlerName)) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue(found, String.format("Method '#%s' referenced in '%s' was not found in controller '%s'",
                            handlerName, path, controllerClass));
                }
            }
        }
    }

    private void collectActionHandlers(Node node, Set<String> handlers) {
        if (node instanceof Element el) {
            String onAction = el.getAttribute("onAction");
            if (onAction != null && onAction.startsWith("#")) {
                handlers.add(onAction.substring(1));
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectActionHandlers(children.item(i), handlers);
        }
    }
}
