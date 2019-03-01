package software.wings.service.impl;

import static io.harness.seeddata.SampleDataProviderConstants.DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_BASIC_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_CANARY_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PIPELINE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PROD_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_QA_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_ROLLING_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import io.harness.seeddata.SampleDataProviderConstants;
import io.harness.seeddata.SampleDataProviderService;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.SampleAppEntityStatus;
import software.wings.beans.SampleAppEntityStatus.Health;
import software.wings.beans.SampleAppStatus;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HarnessSampleAppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.WingsTestConstants;

import java.util.List;

public class HarnessSampeAppServiceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SampleDataProviderService sampleDataProviderService;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;
  @Inject private HarnessSampleAppService harnessSampleAppService;
  @Mock private SettingsService mockSettingsService;
  @Mock private InfrastructureMappingService mockInfrastructureMappingService;

  private void assertSampleAppHealthIsBad(Application sampleApp) {
    boolean isV2 = false;
    // Update entities to check health
    SettingAttribute k8sCloudProvider = settingsService.getSettingAttributeByName(
        WingsTestConstants.ACCOUNT_ID, SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME);
    assertThat(k8sCloudProvider).isNotNull();
    settingsService.update(k8sCloudProvider);

    Service k8sService = serviceResourceService.getServiceByName(sampleApp.getAppId(), K8S_SERVICE_NAME);
    assertThat(k8sService).isNotNull();
    k8sService.setName("Test K8s Service");
    serviceResourceService.update(k8sService);
    isV2 = k8sService.isK8sV2();

    Environment qaEnv = environmentService.getEnvironmentByName(sampleApp.getAppId(), K8S_QA_ENVIRONMENT);
    assertThat(qaEnv).isNotNull();
    qaEnv.setName("Test QA Env");
    environmentService.update(qaEnv);

    Environment prodEnv = environmentService.getEnvironmentByName(sampleApp.getAppId(), K8S_PROD_ENVIRONMENT);
    assertThat(prodEnv).isNotNull();
    prodEnv.setName("Test Prod Env");
    environmentService.update(prodEnv);

    InfrastructureMapping k8sInfraMappingQA = infrastructureMappingService.getInfraMappingByName(
        sampleApp.getAppId(), qaEnv.getUuid(), K8S_SERVICE_INFRA_NAME);
    assertThat(k8sInfraMappingQA).isNotNull();
    k8sInfraMappingQA.setName("Test K8s Service Infra QA");
    when(mockInfrastructureMappingService.update(k8sInfraMappingQA)).thenReturn(k8sInfraMappingQA);

    InfrastructureMapping k8sInfraMappingProd = infrastructureMappingService.getInfraMappingByName(
        sampleApp.getAppId(), prodEnv.getUuid(), K8S_SERVICE_INFRA_NAME);
    assertThat(k8sInfraMappingProd).isNotNull();
    k8sInfraMappingProd.setName("Test K8s Service Infra Prod");
    when(mockInfrastructureMappingService.update(k8sInfraMappingProd)).thenReturn(k8sInfraMappingProd);

    if (isV2) {
      Workflow rollingWf = workflowService.readWorkflowByName(sampleApp.getAppId(), K8S_ROLLING_WORKFLOW_NAME);
      assertThat(rollingWf).isNotNull();
      rollingWf.setName("Test Rolling Workflow");
      workflowService.updateWorkflow(rollingWf);
    } else {
      Workflow basicWf = workflowService.readWorkflowByName(sampleApp.getAppId(), K8S_BASIC_WORKFLOW_NAME);
      assertThat(basicWf).isNotNull();
      basicWf.setName("Test Basic Workflow");
      workflowService.updateWorkflow(basicWf);
    }

    Workflow canaryWf = workflowService.readWorkflowByName(sampleApp.getAppId(), K8S_CANARY_WORKFLOW_NAME);
    assertThat(canaryWf).isNotNull();
    canaryWf.setName("Test Canary Workflow");
    workflowService.updateWorkflow(canaryWf);

    Pipeline k8sPipeline = pipelineService.getPipelineByName(sampleApp.getAppId(), K8S_PIPELINE_NAME);
    assertThat(k8sPipeline).isNotNull();
    k8sPipeline.setName("Test K8s Pipeline");
    pipelineService.update(k8sPipeline);

    SampleAppStatus updatedStatus =
        harnessSampleAppService.getSampleAppHealth(sampleApp.getAccountId(), DeploymentType.KUBERNETES.name());
    assertThat(updatedStatus).isNotNull();
    assertThat(updatedStatus.getDeploymentType().equals(DeploymentType.KUBERNETES.name()));

    // Check health
    List<SampleAppEntityStatus> updatedEntityStatusList = updatedStatus.getStatusList();
    assertThat(updatedEntityStatusList).isNotNull();
    for (SampleAppEntityStatus entity : updatedEntityStatusList) {
      String type = entity.getEntityType();
      String health = entity.getHealth().name();
      if (type.equals(EntityType.APPLICATION.name())) {
        assertThat(health).isEqualTo(Health.GOOD.name());
      }

      if (type.equals(EntityType.SERVICE.name()) || type.equals(EntityType.ARTIFACT_STREAM.name())
          || type.equals(EntityType.ENVIRONMENT.name()) || type.equals(EntityType.SERVICE_TEMPLATE.name())
          || type.equals(EntityType.INFRASTRUCTURE_MAPPING.name()) || type.equals(EntityType.WORKFLOW.name())
          || type.equals(EntityType.PIPELINE.name())) {
        assertThat(health).isEqualTo(Health.BAD.name());
      }
    }
  }

  @Test
  public void ensureSampleAppHealthIsBad() {
    // Create a sample app v1
    Application sampleAppV1 = createHarnessSampleApp();
    assertThat(sampleAppV1).isNotNull();

    // Get app status
    SampleAppStatus sampleAppV1Status =
        harnessSampleAppService.getSampleAppHealth(sampleAppV1.getAccountId(), DeploymentType.KUBERNETES.name());
    assertThat(sampleAppV1Status).isNotNull();
    assertThat(sampleAppV1Status.getDeploymentType().equals(DeploymentType.KUBERNETES.name()));

    assertSampleAppHealthIsBad(sampleAppV1);

    Application existingApp = appService.getAppByName(sampleAppV1.getAccountId(), HARNESS_SAMPLE_APP);
    if (existingApp != null) {
      appService.delete(existingApp.getAppId());
    }

    // Create a sample app v2
    Application sampleAppV2 = createHarnessSampleAppV2();
    assertThat(sampleAppV2).isNotNull();

    // Get app status
    SampleAppStatus sampleAppV2Status =
        harnessSampleAppService.getSampleAppHealth(sampleAppV1.getAccountId(), DeploymentType.KUBERNETES.name());
    assertThat(sampleAppV2Status).isNotNull();
    assertThat(sampleAppV2Status.getDeploymentType().equals(DeploymentType.KUBERNETES.name()));

    assertSampleAppHealthIsBad(sampleAppV2);
  }

  private void assertSampleAppIsGood(Application sampleApp) {
    // Get app status
    SampleAppStatus sampleAppStatus =
        harnessSampleAppService.getSampleAppHealth(sampleApp.getAccountId(), DeploymentType.KUBERNETES.name());
    assertThat(sampleAppStatus).isNotNull();
    assertThat(sampleAppStatus.getDeploymentType().equals(DeploymentType.KUBERNETES.name()));

    List<SampleAppEntityStatus> entityStatusList = sampleAppStatus.getStatusList();
    for (SampleAppEntityStatus entity : entityStatusList) {
      String type = entity.getEntityType();
      String name = entity.getEntityName();
      String health = entity.getHealth().name();

      assertThat(health).isEqualTo(Health.GOOD.name());
      if (type.equals(EntityType.APPLICATION.name())) {
        assertThat(name).isEqualTo(HARNESS_SAMPLE_APP);
      }
      if (type.equals(Category.CLOUD_PROVIDER.name())) {
        assertThat(name).isEqualTo(K8S_CLOUD_PROVIDER_NAME);
      }
      if (type.equals(Category.CONNECTOR.name())) {
        assertThat(name).isEqualTo(HARNESS_DOCKER_HUB_CONNECTOR);
      }
      if (type.equals(EntityType.SERVICE.name())) {
        assertThat(name).isEqualTo(K8S_SERVICE_NAME);
      }
      if (type.equals(EntityType.ARTIFACT_STREAM.name())) {
        assertThat(name).isEqualTo(DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME);
      }
      if (type.equals(EntityType.ENVIRONMENT.name())) {
        assertThat(name).isIn(K8S_QA_ENVIRONMENT, K8S_PROD_ENVIRONMENT);
      }
      if (type.equals(EntityType.SERVICE_TEMPLATE.name()) || type.equals(EntityType.INFRASTRUCTURE_MAPPING.name())) {
        assertThat(name).isEqualTo(K8S_SERVICE_INFRA_NAME);
      }
      if (type.equals(EntityType.WORKFLOW.name())) {
        assertThat(name).isIn(K8S_BASIC_WORKFLOW_NAME, K8S_CANARY_WORKFLOW_NAME, K8S_ROLLING_WORKFLOW_NAME);
      }
      if (type.equals(EntityType.PIPELINE.name())) {
        assertThat(name).isEqualTo(K8S_PIPELINE_NAME);
      }
    }
  }

  @Test
  public void ensureSampleAppHealthIsGood() {
    // Create a sample app v1
    Application sampleAppV1 = createHarnessSampleApp();
    assertThat(sampleAppV1).isNotNull();
    assertSampleAppIsGood(sampleAppV1);

    Application sampleAppV2 = createHarnessSampleAppV2();
    assertThat(sampleAppV2).isNotNull();
    assertSampleAppIsGood(sampleAppV2);
  }

  @Test
  public void ensureSampleAppRestore() {
    // Create a sample app
    Application sampleApp = createHarnessSampleApp();
    assertThat(sampleApp).isNotNull();

    sampleApp.setName("Updated Sample App name");
    appService.update(sampleApp);

    // Get app health
    SampleAppStatus sampleAppStatus =
        harnessSampleAppService.getSampleAppHealth(sampleApp.getAccountId(), DeploymentType.KUBERNETES.name());
    assertThat(sampleAppStatus).isNotNull();
    assertThat(sampleAppStatus.getDeploymentType().equals(DeploymentType.KUBERNETES.name()));
    assertThat(sampleAppStatus.getHealth()).isEqualTo(Health.BAD);

    // Restore app
    Application dummy = createHarnessSampleApp();
    Application restoredApp =
        harnessSampleAppService.restoreSampleApp(sampleApp.getAccountId(), DeploymentType.KUBERNETES.name(), dummy);
    assertThat(restoredApp).isNotNull();

    // Get app health again
    SampleAppStatus restoredAppStatus =
        harnessSampleAppService.getSampleAppHealth(restoredApp.getAccountId(), DeploymentType.KUBERNETES.name());
    assertThat(restoredAppStatus.getDeploymentType().equals(DeploymentType.KUBERNETES.name()));
    assertThat(restoredAppStatus.getHealth()).isEqualTo(Health.GOOD);

    // Restored app is always v2
    assertThat(restoredApp.getServices().stream().allMatch(service -> service.isK8sV2() == true));
  }

  private Application createHarnessSampleApp() {
    Account savedAccount = wingsPersistence.saveAndGet(Account.class,
        anAccount().withAccountName(WingsTestConstants.ACCOUNT_NAME).withUuid(WingsTestConstants.ACCOUNT_ID).build());
    assertThat(savedAccount).isNotNull();

    sampleDataProviderService.createHarnessSampleApp(savedAccount);
    Application app = appService.getAppByName(savedAccount.getUuid(), SampleDataProviderConstants.HARNESS_SAMPLE_APP);
    assertThat(app).isNotNull();
    return app;
  }

  private Application createHarnessSampleAppV2() {
    Account savedAccount = wingsPersistence.saveAndGet(Account.class,
        anAccount().withAccountName(WingsTestConstants.ACCOUNT_NAME).withUuid(WingsTestConstants.ACCOUNT_ID).build());
    assertThat(savedAccount).isNotNull();

    sampleDataProviderService.createK8sV2SampleApp(savedAccount);
    Application app = appService.getAppByName(savedAccount.getUuid(), SampleDataProviderConstants.HARNESS_SAMPLE_APP);
    assertThat(app).isNotNull();
    return app;
  }
}
