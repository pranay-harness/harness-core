package io.harness.seeddata;

import static io.harness.seeddata.SeedDataProviderConstants.DOCKER_CONNECTOR_NAME;
import static io.harness.seeddata.SeedDataProviderConstants.KUBERNETES_APP_NAME;
import static io.harness.seeddata.SeedDataProviderConstants.KUBERNETES_SERVICE_INFRA_NAME;
import static io.harness.seeddata.SeedDataProviderConstants.KUBERNETES_SERVICE_NAME;
import static io.harness.seeddata.SeedDataProviderConstants.KUBE_CLUSTER_NAME;
import static io.harness.seeddata.SeedDataProviderConstants.KUBE_PIPELINE_NAME;
import static io.harness.seeddata.SeedDataProviderConstants.KUBE_PROD_ENVIRONMENT;
import static io.harness.seeddata.SeedDataProviderConstants.KUBE_QA_ENVIRONMENT;
import static io.harness.seeddata.SeedDataProviderConstants.KUBE_WORKFLOW_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.WingsTestConstants;

public class SeedDataProviderServiceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SeedDataProviderService seedDataProviderService;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;

  @Test
  public void shouldCreateSeedKubernetesApp() {
    Account savedAccount = wingsPersistence.saveAndGet(Account.class,
        anAccount().withAccountName(WingsTestConstants.ACCOUNT_NAME).withUuid(WingsTestConstants.ACCOUNT_ID).build());

    assertThat(savedAccount).isNotNull();

    Application kubernetesApp = seedDataProviderService.createKubernetesApp(savedAccount);

    assertThat(kubernetesApp).isNotNull();

    // Verify the Kube cluster cloud provider
    assertThat(settingsService.getSettingAttributeByName(savedAccount.getUuid(), KUBE_CLUSTER_NAME)).isNotNull();
    // Verify the connector created
    assertThat(settingsService.getSettingAttributeByName(savedAccount.getUuid(), DOCKER_CONNECTOR_NAME)).isNotNull();
    // Verify the app
    assertThat(appService.getAppByName(savedAccount.getUuid(), KUBERNETES_APP_NAME)).isNotNull();
    // Verify  service
    assertThat(serviceResourceService.getServiceByName(kubernetesApp.getUuid(), KUBERNETES_SERVICE_NAME)).isNotNull();
    // Verify environment
    Environment qaEnv = environmentService.getEnvironmentByName(kubernetesApp.getUuid(), KUBE_QA_ENVIRONMENT);
    assertThat(qaEnv).isNotNull();
    Environment prodEnv = environmentService.getEnvironmentByName(kubernetesApp.getUuid(), KUBE_PROD_ENVIRONMENT);
    assertThat(prodEnv).isNotNull();

    // Verify inframapping
    assertThat(infrastructureMappingService.getInfraMappingByName(
                   kubernetesApp.getAppId(), qaEnv.getUuid(), KUBERNETES_SERVICE_INFRA_NAME))
        .isNotNull();

    assertThat(infrastructureMappingService.getInfraMappingByName(
                   kubernetesApp.getAppId(), prodEnv.getUuid(), KUBERNETES_SERVICE_INFRA_NAME))
        .isNotNull();

    // Verify workflow
    assertThat(workflowService.readWorkflowByName(kubernetesApp.getAppId(), KUBE_WORKFLOW_NAME)).isNotNull();

    // Verify pipeline
    assertThat(pipelineService.getPipelineByName(kubernetesApp.getAppId(), KUBE_PIPELINE_NAME)).isNotNull();
  }
}