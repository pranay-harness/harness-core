package io.harness.seeddata;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_BASIC_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_CANARY_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_INFRA_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PIPELINE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PROD_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_QA_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_ROLLING_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
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
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
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
    assertThat(serviceResourceService.getServiceByName(app.getUuid(), K8S_SERVICE_NAME)).isNotNull();
    // Verify environment
    Environment qaEnv = environmentService.getEnvironmentByName(app.getUuid(), K8S_QA_ENVIRONMENT);
    assertThat(qaEnv).isNotNull();
    Environment prodEnv = environmentService.getEnvironmentByName(app.getUuid(), K8S_PROD_ENVIRONMENT);
    assertThat(prodEnv).isNotNull();

    // Verify inframapping
    assertThat(
        infrastructureMappingService.getInfraMappingByName(app.getAppId(), qaEnv.getUuid(), K8S_SERVICE_INFRA_NAME))
        .isNotNull();

    assertThat(
        infrastructureMappingService.getInfraMappingByName(app.getAppId(), prodEnv.getUuid(), K8S_SERVICE_INFRA_NAME))
        .isNotNull();

    // Verify Basic workflow
    assertThat(workflowService.readWorkflowByName(app.getAppId(), K8S_BASIC_WORKFLOW_NAME)).isNotNull();

    // Verify Canary workflow
    assertThat(workflowService.readWorkflowByName(app.getAppId(), K8S_CANARY_WORKFLOW_NAME)).isNotNull();

    // Verify pipeline
    assertThat(pipelineService.getPipelineByName(app.getAppId(), K8S_PIPELINE_NAME)).isNotNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCreateSampleAppWithInfraDefinitions() {
    Account account =
        anAccount().withAccountName(WingsTestConstants.ACCOUNT_NAME).withUuid(WingsTestConstants.ACCOUNT_ID).build();
    String accountId = wingsPersistence.save(account);

    assertThat(accountId).isNotNull();

    wingsPersistence.save(FeatureFlag.builder().name(FeatureName.INFRA_MAPPING_REFACTOR.name()).enabled(true).build());

    sampleDataProviderService.createK8sV2SampleApp(account);

    final Application app = appService.getAppByName(account.getUuid(), SampleDataProviderConstants.HARNESS_SAMPLE_APP);
    assertThat(app).isNotNull();

    // Verify the Kube cluster cloud provider
    assertThat(settingsService.getSettingAttributeByName(
                   account.getUuid(), SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME))
        .isNotNull();
    // Verify the connector created
    assertThat(settingsService.getSettingAttributeByName(
                   account.getUuid(), SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR))
        .isNotNull();
    // Verify the app
    assertThat(appService.getAppByName(account.getUuid(), HARNESS_SAMPLE_APP)).isNotNull();
    // Verify  service
    assertThat(serviceResourceService.getServiceByName(app.getUuid(), K8S_SERVICE_NAME)).isNotNull();
    // Verify environment
    Environment qaEnv = environmentService.getEnvironmentByName(app.getUuid(), K8S_QA_ENVIRONMENT);
    assertThat(qaEnv).isNotNull();
    Environment prodEnv = environmentService.getEnvironmentByName(app.getUuid(), K8S_PROD_ENVIRONMENT);
    assertThat(prodEnv).isNotNull();

    // Verify infra definition
    assertThat(infrastructureDefinitionService.getInfraDefByName(app.getAppId(), qaEnv.getUuid(), K8S_INFRA_NAME))
        .isNotNull();

    assertThat(infrastructureDefinitionService.getInfraDefByName(app.getAppId(), prodEnv.getUuid(), K8S_INFRA_NAME))
        .isNotNull();

    // Verify Basic workflow
    assertThat(workflowService.readWorkflowByName(app.getAppId(), K8S_ROLLING_WORKFLOW_NAME)).isNotNull();

    // Verify Canary workflow
    assertThat(workflowService.readWorkflowByName(app.getAppId(), K8S_CANARY_WORKFLOW_NAME)).isNotNull();

    // Verify pipeline
    assertThat(pipelineService.getPipelineByName(app.getAppId(), K8S_PIPELINE_NAME)).isNotNull();
  }
}