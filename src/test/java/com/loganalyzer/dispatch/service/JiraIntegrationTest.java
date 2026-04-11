package com.loganalyzer.dispatch.service;

import com.loganalyzer.analysis.entity.Anomaly;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

class JiraIntegrationTest {

    @Test
    void testCreateJiraBug() {
        // Create an instance without starting the full Spring Application Context
        JiraTicketService jiraTicketService = new JiraTicketService();

        // Load properties explicitly to avoid Spring Boot scanning issues
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = JiraIntegrationTest.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("Could not find application.properties!");
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        // Inject your real properties explicitly so Jira can authenticate
        ReflectionTestUtils.setField(jiraTicketService, "jiraBaseUrl", props.getProperty("jira.base-url"));
        ReflectionTestUtils.setField(jiraTicketService, "jiraUsername", props.getProperty("jira.username"));
        ReflectionTestUtils.setField(jiraTicketService, "jiraApiToken", props.getProperty("jira.api-token"));
        ReflectionTestUtils.setField(jiraTicketService, "jiraProjectKey", props.getProperty("jira.project-key"));

        // Create a dummy Anomaly to test Jira creation
        Anomaly dummyAnomaly = new Anomaly();
        dummyAnomaly.setId(UUID.randomUUID().toString());
        dummyAnomaly.setServiceName("TEST_SERVICE");
        dummyAnomaly.setMatchedRuleId("TEST_RULE_BUG");
        dummyAnomaly.setSource("Manual Integration Test");
        dummyAnomaly.setLogContent("This is a test log message ensuring our Bug creation integration works!");
        dummyAnomaly.setSeverityLevel("HIGH");

        // Fire the code
        System.out.println("Beginning test: Dispatching test anomaly to Jira...");
        jiraTicketService.dispatch(dummyAnomaly);
        System.out.println("Finished test dispatch logic. Check your Jira instance to see if the ticket appeared!");
    }
}
