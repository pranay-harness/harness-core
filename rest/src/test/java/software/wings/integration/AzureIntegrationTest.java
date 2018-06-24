package software.wings.integration;

import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureAvailabilitySet;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureTagDetails;
import software.wings.beans.AzureVirtualMachineScaleSet;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.rules.Integration;
import software.wings.service.impl.security.EncryptionServiceImpl;

import java.util.Collections;
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

  private static final String clientId = "placeholder";
  private static final String tenantId = "placeholder";
  private static final String key = "placeholder";
  private static final String subscriptionId = "placeholder";

  private static AzureHelperService azureHelperService = new AzureHelperService();

  public static void setUp() {
    setInternalState(azureHelperService, "encryptionService", new EncryptionServiceImpl());
  }

  public static void main(String[] args) {
    logger.info("AzureIntegrationTest: Start.");
    setUp();
    azureAuthenticationTest();
    getSubscriptions();
    getContainerRegistries();
    getRepositoryTags();
    getKubernetesClusters();
    getKubernetesClusterConfig();
    getAvailableTags();
    getVirtualMachineScaleSets();
    getAvailabilitySets();
    logger.info("AzureIntegrationTest: Done.");
  }

  private static void azureAuthenticationTest() {
    azureHelperService.validateAzureAccountCredential(clientId, tenantId, key);
  }

  private static void getSubscriptions() {
    AzureConfig config = getAzureConfig();
    logger.info(
        "Azure Subscriptions: " + azureHelperService.listSubscriptions(config, Collections.emptyList()).toString());
  }

  private static void getContainerRegistries() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<String> registries =
          azureHelperService.listContainerRegistries(config, Collections.emptyList(), subscriptionId);
      for (String registry : registries) {
        logger.info("Details: " + subscriptionId + " " + registry);
      }
    }
  }

  private static void getVirtualMachineScaleSets() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<AzureVirtualMachineScaleSet> virtualMachineScaleSets =
          azureHelperService.listVirtualMachineScaleSets(config, Collections.emptyList(), subscriptionId);
      for (AzureVirtualMachineScaleSet vmss : virtualMachineScaleSets) {
        logger.info("Details: " + subscriptionId + " " + vmss.getResourceId());
      }
    }
  }

  private static void getAvailabilitySets() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<AzureAvailabilitySet> availabilitySets =
          azureHelperService.listAvailabilitySets(config, Collections.emptyList(), subscriptionId);
      for (AzureAvailabilitySet as : availabilitySets) {
        logger.info("Details: " + subscriptionId + " " + as.getResourceId());
      }
    }
  }

  private static void getAvailableTags() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      logger.info("Subscription: " + subscriptionId);
      List<AzureTagDetails> tags = azureHelperService.listTags(config, Collections.emptyList(), subscriptionId);
      for (AzureTagDetails tag : tags) {
        logger.info("Tag: " + tag.getTagName());
        for (String value : tag.getValues()) {
          logger.info("Value: " + value);
        }
      }
    }
  }

  private static void getRepositoryTags() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<String> registries =
          azureHelperService.listContainerRegistries(config, Collections.emptyList(), subscriptionId);
      for (String registry : registries) {
        List<String> repositories =
            azureHelperService.listRepositories(config, Collections.emptyList(), subscriptionId, registry);
        for (String repository : repositories) {
          List<String> tags = azureHelperService.listRepositoryTags(
              config, Collections.emptyList(), subscriptionId, registry, repository);
          logger.info("Details: " + subscriptionId + " " + registry + " " + repository + " " + tags.toString());
        }
      }
    }
  }

  private static void getKubernetesClusters() {
    AzureConfig config = getAzureConfig();
    List<AzureKubernetesCluster> clusters =
        azureHelperService.listKubernetesClusters(config, Collections.emptyList(), subscriptionId);
    logger.info("Clusters:");
    clusters.forEach(cluster -> logger.info("Cluster Detail: " + cluster.getResourceGroup() + "/" + cluster.getName()));
  }

  private static void getKubernetesClusterConfig() {
    AzureConfig config = getAzureConfig();
    azureHelperService.getKubernetesClusterConfig(
        config, Collections.emptyList(), subscriptionId, "puneet-aks", "puneet-aks", "default");
  }

  private static AzureConfig getAzureConfig() {
    return new AzureConfig(clientId, tenantId, key.toCharArray(), "", "");
  }
}