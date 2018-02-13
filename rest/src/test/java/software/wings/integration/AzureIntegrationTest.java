package software.wings.integration;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureConfig;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.rules.Integration;

import java.util.List;
import java.util.Map;

@Integration
@Ignore
/*
This test class is making calls to Azure. The purpose is to do directed testing for Azure APIs.
This is not to be run as an automated test[hence the @Ignore].
 */
public class AzureIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(AzureIntegrationTest.class);

  private static final String clientId = "a19170f2-a2b1-49d2-9a0f-091b02fb7cb7";
  private static final String tenantId = "bd57732b-0443-4542-887b-e601cff640a1";
  private static final String key = "aqFVAJLuVL7fyOyv0OPW6N8HTyQWr1366t1smLIDfXA=";
  private static final String subscriptionId = "12d2db62-5aa9-471d-84bb-faa489b3e319";

  public static void main(String[] args) {
    logger.info("AzureIntegrationTest: Start.");
    azureAuthenticationTest();
    getSubscriptions();
    getContainerRegistries();
    getRepositoryTags();
    logger.info("AzureIntegrationTest: Done.");
  }

  private static void azureAuthenticationTest() {
    AzureHelperService azure = new AzureHelperService();
    azure.validateAzureAccountCredential(clientId, tenantId, key);
  }

  private static void getSubscriptions() {
    AzureHelperService azure = new AzureHelperService();
    AzureConfig config = getAzureConfig();
    logger.info("Azure Subscriptions: " + azure.listSubscriptions(config).toString());
  }

  private static void getContainerRegistries() {
    AzureHelperService azure = new AzureHelperService();
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azure.listSubscriptions(config);
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<String> registries = azure.listContainerRegistries(config, subscriptionId);
      for (String registry : registries) {
        logger.info("Details: " + subscriptionId + " " + registry);
      }
    }
  }

  private static void getRepositoryTags() {
    AzureHelperService azure = new AzureHelperService();
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azure.listSubscriptions(config);
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<String> registries = azure.listContainerRegistries(config, subscriptionId);
      for (String registry : registries) {
        List<String> repositories = azure.listRepositories(config, subscriptionId, registry);
        for (String repository : repositories) {
          List<String> tags = azure.listRepositoryTags(config, subscriptionId, registry, repository);
          logger.info("Details: " + subscriptionId + " " + registry + " " + repository + " " + tags.toString());
        }
      }
    }
  }

  private static AzureConfig getAzureConfig() {
    return new AzureConfig(clientId, tenantId, key, "");
  }
}