package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import com.microsoft.azure.management.compute.VirtualMachine;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureAvailabilitySet;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureVirtualMachineScaleSet;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.rules.Integration;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Integration
public class AzureIntegrationTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(AzureIntegrationTest.class);

  private String clientId = "";
  private String tenantId = "";
  private String key = "";
  private final String subscriptionId = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0"; // Harness-QA subscription

  @Inject private ScmSecret scmSecret;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EncryptionService encryptionService;

  @Before
  public void setUp() {
    setInternalState(azureHelperService, "encryptionService", encryptionService);
    clientId = scmSecret.decryptToString(new SecretName("azure_client_id"));
    tenantId = scmSecret.decryptToString(new SecretName("azure_tenant_id"));
    key = scmSecret.decryptToString(new SecretName("azure_key"));
  }

  @Test
  public void azureAuthenticationTest() {
    azureHelperService.validateAzureAccountCredential(clientId, tenantId, key);
  }

  @Test
  public void getSubscriptionsTest() {
    AzureConfig config = getAzureConfig();

    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    assertThat(subscriptions).isNotEmpty();
    logger.info("Azure Subscriptions: " + subscriptions);
  }

  @Test
  public void getContainerRegistriesTest() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    List<String> registries = new ArrayList<>();

    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      registries.addAll(azureHelperService.listContainerRegistries(config, Collections.emptyList(), subscriptionId));
    }
    assertThat(registries).isNotEmpty();

    logger.info("Azure Container Registries: " + registries);
  }

  @Test
  public void getVirtualMachineScaleSetsTest() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<AzureVirtualMachineScaleSet> virtualMachineScaleSets =
          azureHelperService.listVirtualMachineScaleSets(config, Collections.emptyList(), subscriptionId);
      for (AzureVirtualMachineScaleSet vmss : virtualMachineScaleSets) {
        logger.info("Details: " + subscriptionId + " " + vmss.getResourceId());
      }
    }
  }

  @Test
  public void getAvailabilitySets() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<AzureAvailabilitySet> availabilitySets =
          azureHelperService.listAvailabilitySets(config, Collections.emptyList(), subscriptionId);
      for (AzureAvailabilitySet as : availabilitySets) {
        logger.info("Details: " + subscriptionId + " " + as.getResourceId());
      }
    }
  }

  @Test
  public void getAvailableTags() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      logger.info("Subscription: " + subscriptionId);
      Set<String> tags = azureHelperService.listTagsBySubscription(subscriptionId, config, Collections.emptyList());
      assertThat(tags).isNotEmpty();
      for (String tag : tags) {
        logger.info("Tag: " + tag);
      }
    }
  }

  @Test
  public void getResourceGroups() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      logger.info("Subscription: " + subscriptionId);
      Set<String> resourceGroups =
          azureHelperService.listResourceGroups(config, Collections.EMPTY_LIST, subscriptionId);
      assertThat(resourceGroups).isNotEmpty();
      for (String rg : resourceGroups) {
        logger.info("Resource group : " + rg);
      }
    }
  }

  @Test
  public void getVmsByResourceGroupAndTag() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = azureHelperService.listSubscriptions(config, Collections.emptyList());
    subscriptions.forEach((subId, Desc) -> logger.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      logger.info("Subscription: " + subscriptionId);
      Set<String> resourceGroups =
          azureHelperService.listResourceGroups(config, Collections.EMPTY_LIST, subscriptionId);
      assertThat(resourceGroups).isNotEmpty();

      for (String rg : resourceGroups) {
        List<VirtualMachine> vms = azureHelperService.listVmsByTagsAndResourceGroup(
            config, Collections.EMPTY_LIST, subscriptionId, rg, Collections.EMPTY_MAP);
        for (VirtualMachine vm : vms) {
          logger.info("Resource group :" + rg + " VM :" + vm.name());
        }
      }
    }
  }

  @Test
  public void getRepositoryTags() {
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

  @Test
  public void getKubernetesClusters() {
    AzureConfig config = getAzureConfig();
    List<AzureKubernetesCluster> clusters =
        azureHelperService.listKubernetesClusters(config, Collections.emptyList(), subscriptionId);

    assertThat(clusters).isNotEmpty();

    clusters.forEach(cluster -> {
      logger.info("Cluster Detail: " + cluster.getResourceGroup() + "/" + cluster.getName());
      azureHelperService.getKubernetesClusterConfig(
          config, Collections.emptyList(), subscriptionId, cluster.getResourceGroup(), cluster.getName(), "default");
    });
  }

  private AzureConfig getAzureConfig() {
    return new AzureConfig(clientId, tenantId, key.toCharArray(), "", "");
  }
}