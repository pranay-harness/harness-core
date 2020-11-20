package software.wings.integration;

import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.containerservice.OSType;
import io.harness.beans.AzureEnvironmentType;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.ccm.config.CCMConfig;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
@Slf4j
public class AzureIntegrationTest extends WingsBaseTest {
  private String clientId = "";
  private String tenantId = "";
  private String key = "";
  private final String subscriptionId = "20d6a917-99fa-4b1b-9b2e-a3d624e9dcf0"; // Harness-QA subscription

  @Inject private ScmSecret scmSecret;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EncryptionService encryptionService;

  @Before
  public void setUp() throws IllegalAccessException {
    FieldUtils.writeField(azureHelperService, "encryptionService", encryptionService, true);
    clientId = scmSecret.decryptToString(new SecretName("azure_client_id"));
    tenantId = scmSecret.decryptToString(new SecretName("azure_tenant_id"));
    key = scmSecret.decryptToString(new SecretName("azure_key"));
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void azureAuthenticationTest() {
    AzureConfig config = getAzureConfig();
    azureHelperService.validateAzureAccountCredential(config, Collections.emptyList());
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getSubscriptionsTest() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    assertThat(subscriptions).isNotEmpty();
    log.info("Azure Subscriptions: " + subscriptions);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getContainerRegistriesTest() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    List<String> registries = new ArrayList<>();

    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      registries.addAll(azureHelperService.listContainerRegistryNames(config, Collections.emptyList(), subscriptionId));
    }
    assertThat(registries).isNotEmpty();

    log.info("Azure Container Registries: " + registries);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getVirtualMachineScaleSetsTest() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<AzureVirtualMachineScaleSet> virtualMachineScaleSets =
          azureHelperService.listVirtualMachineScaleSets(config, Collections.emptyList(), subscriptionId);
      for (AzureVirtualMachineScaleSet vmss : virtualMachineScaleSets) {
        log.info("Details: " + subscriptionId + " " + vmss.getResourceId());
      }
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getAvailabilitySets() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<AzureAvailabilitySet> availabilitySets =
          azureHelperService.listAvailabilitySets(config, Collections.emptyList(), subscriptionId);
      for (AzureAvailabilitySet as : availabilitySets) {
        log.info("Details: " + subscriptionId + " " + as.getResourceId());
      }
    }
  }

  @Test
  @Owner(developers = PUNEET, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getAvailableTags() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    subscriptions.forEach((subId, Desc) -> log.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      log.info("Subscription: " + subscriptionId);
      Set<String> tags = azureHelperService.listTagsBySubscription(subscriptionId, config, Collections.emptyList());
      assertThat(tags).isNotEmpty();
      for (String tag : tags) {
        log.info("Tag: " + tag);
      }
    }
  }

  @Test
  @Owner(developers = PUNEET, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("Ignoring for now as its flaky")
  public void getHostsByResourceGroupAndTag() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    subscriptions.forEach((subId, Desc) -> log.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      log.info("Subscription: " + subscriptionId);
      Set<String> resourceGroups =
          azureHelperService.listResourceGroups(config, Collections.EMPTY_LIST, subscriptionId);
      assertThat(resourceGroups).isNotEmpty();

      // Win hosts
      for (String rg : resourceGroups) {
        List<VirtualMachine> vms = azureHelperService.listVmsByTagsAndResourceGroup(
            config, Collections.EMPTY_LIST, subscriptionId, rg, Collections.EMPTY_MAP, OSType.WINDOWS);
        // assert that only windows instances are returned
        for (VirtualMachine vm : vms) {
          assertThat(vm.inner().osProfile().windowsConfiguration()).isNotNull();
          log.info("Resource group :" + rg + " VM :" + vm.name());
        }
      }

      // Linux hosts
      for (String rg : resourceGroups) {
        List<VirtualMachine> vms = azureHelperService.listVmsByTagsAndResourceGroup(
            config, Collections.EMPTY_LIST, subscriptionId, rg, Collections.EMPTY_MAP, OSType.LINUX);
        // assert that only linux instances are returned
        for (VirtualMachine vm : vms) {
          assertThat(vm.inner().osProfile().linuxConfiguration()).isNotNull();
          log.info("Resource group :" + rg + " VM :" + vm.name());
        }
      }
    }
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getRepositoryTags() {
    AzureConfig config = getAzureConfig();
    Map<String, String> subscriptions = listSubscriptions(config);
    subscriptions.forEach((subId, Desc) -> log.info(subId + Desc));
    for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
      String subscriptionId = entry.getKey();
      List<String> registries =
          azureHelperService.listContainerRegistryNames(config, Collections.emptyList(), subscriptionId);
      for (String registry : registries) {
        if (registry.equals("harnessexample")) {
          List<String> repositories =
              azureHelperService.listRepositories(config, Collections.emptyList(), subscriptionId, registry);
          for (String repository : repositories) {
            List<String> tags = azureHelperService.listRepositoryTags(
                config, Collections.emptyList(), subscriptionId, registry, repository);
            log.info("Details: " + subscriptionId + " " + registry + " " + repository + " " + tags.toString());
          }
        }
      }
    }
  }

  @Test
  @Owner(developers = PUNEET, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void getKubernetesClusters() {
    AzureConfig config = getAzureConfig();
    List<AzureKubernetesCluster> clusters =
        azureHelperService.listKubernetesClusters(config, Collections.emptyList(), subscriptionId);

    assertThat(clusters).isNotEmpty();

    clusters.forEach(cluster -> {
      log.info("Cluster Detail: " + cluster.getResourceGroup() + "/" + cluster.getName());
      azureHelperService.getKubernetesClusterConfig(config, Collections.emptyList(), subscriptionId,
          cluster.getResourceGroup(), cluster.getName(), "default", false);
    });
  }

  private AzureConfig getAzureConfig() {
    CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(false).build();
    return new AzureConfig(clientId, tenantId, key.toCharArray(), "", ccmConfig, "", AzureEnvironmentType.AZURE);
  }

  private Map<String, String> listSubscriptions(AzureConfig config) {
    return azureHelperService.listSubscriptions(config, Collections.emptyList());
  }
}
