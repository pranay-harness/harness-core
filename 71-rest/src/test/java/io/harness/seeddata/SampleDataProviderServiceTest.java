package io.harness.seeddata;

import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.KUBERNETES_SERVICE_INFRA_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.KUBERNETES_SERVICE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.KUBE_PIPELINE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.KUBE_PROD_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.KUBE_QA_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.KUBE_WORKFLOW_NAME;
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

public class SampleDataProviderServiceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SampleDataProviderService sampleDataProviderService;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;

  @Test
  public void shouldCreateSampleApp() {
    Account savedAccount = wingsPersistence.saveAndGet(Account.class,
        anAccount().withAccountName(WingsTestConstants.ACCOUNT_NAME).withUuid(WingsTestConstants.ACCOUNT_ID).build());

    assertThat(savedAccount).isNotNull();

    sampleDataProviderService.createHarnessSampleApp(savedAccount);

    final Application app =
        appService.getAppByName(savedAccount.getUuid(), SampleDataProviderConstants.HARNESS_SAMPLE_APP);
    assertThat(app).isNotNull();

    // Verify the Kube cluster cloud provider
    assertThat(settingsService.getSettingAttributeByName(
                   savedAccount.getUuid(), SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME))
        .isNotNull();
    // Verify the connector created
    assertThat(settingsService.getSettingAttributeByName(
                   savedAccount.getUuid(), SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR))
        .isNotNull();
    // Verify the app
    assertThat(appService.getAppByName(savedAccount.getUuid(), HARNESS_SAMPLE_APP)).isNotNull();
    // Verify  service
    assertThat(serviceResourceService.getServiceByName(app.getUuid(), KUBERNETES_SERVICE_NAME)).isNotNull();
    // Verify environment
    Environment qaEnv = environmentService.getEnvironmentByName(app.getUuid(), KUBE_QA_ENVIRONMENT);
    assertThat(qaEnv).isNotNull();
    Environment prodEnv = environmentService.getEnvironmentByName(app.getUuid(), KUBE_PROD_ENVIRONMENT);
    assertThat(prodEnv).isNotNull();

    // Verify inframapping
    assertThat(infrastructureMappingService.getInfraMappingByName(
                   app.getAppId(), qaEnv.getUuid(), KUBERNETES_SERVICE_INFRA_NAME))
        .isNotNull();

    assertThat(infrastructureMappingService.getInfraMappingByName(
                   app.getAppId(), prodEnv.getUuid(), KUBERNETES_SERVICE_INFRA_NAME))
        .isNotNull();

    // Verify workflow
    assertThat(workflowService.readWorkflowByName(app.getAppId(), KUBE_WORKFLOW_NAME)).isNotNull();

    // Verify pipeline
    assertThat(pipelineService.getPipelineByName(app.getAppId(), KUBE_PIPELINE_NAME)).isNotNull();
  }
}