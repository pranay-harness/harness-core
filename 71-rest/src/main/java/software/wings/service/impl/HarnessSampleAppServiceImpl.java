package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.seeddata.SampleDataProviderConstants.DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_DOCKER_HUB_CONNECTOR;
import static io.harness.seeddata.SampleDataProviderConstants.HARNESS_SAMPLE_APP;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_CANARY_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_CLOUD_PROVIDER_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PIPELINE_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PROD_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_QA_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_ROLLING_WORKFLOW_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_INFRA_NAME;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_SERVICE_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.seeddata.SampleDataProviderService;
import org.hibernate.validator.constraints.NotEmpty;
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
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HarnessSampleAppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class HarnessSampleAppServiceImpl implements HarnessSampleAppService {
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject public ArtifactStreamService artifactStreamService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private SampleDataProviderService sampleDataProviderService;
  @Inject private AccountService accountService;
  @Inject private AuthService authService;

  @Override
  public SampleAppStatus getSampleAppHealth(String accountId, String deploymentType) {
    if (isEmpty(deploymentType)) {
      throw new WingsException("Please specify deployment type for sample app.", WingsException.USER);
    }
    if (deploymentType.equals(DeploymentType.KUBERNETES.name())) {
      return isK8sApplicationHealthy(accountId);
    } else {
      throw new WingsException("Sample App for deployment type not supported." + deploymentType, WingsException.USER);
    }
  }

  @Override
  public Application restoreSampleApp(@NotEmpty String accountId, String deploymentType, Application application) {
    if (isEmpty(deploymentType)) {
      throw new WingsException("Please specify deployment type for restoring sample app.", WingsException.USER);
    }
    if (!deploymentType.equals(DeploymentType.KUBERNETES.name())) {
      throw new WingsException("Sample App for deployment type not supported." + deploymentType, WingsException.USER);
    }

    // Clean up existing app
    cleanUpSampleApp(accountId);
    authService.evictUserPermissionAndRestrictionCacheForAccount(accountId, true, true);

    // Create a new k8s sample app
    Account account = accountService.get(accountId);
    if (account != null) {
      sampleDataProviderService.createK8sV2SampleApp(account);
      authService.evictUserPermissionAndRestrictionCacheForAccount(accountId, true, true);
    } else {
      throw new WingsException(
          "Account invalid. Sample App for deployment type could not be restored." + deploymentType,
          WingsException.USER);
    }
    return appService.getAppByName(accountId, HARNESS_SAMPLE_APP);
  }

  private void cleanUpSampleApp(String accountId) {
    Application existingApp = appService.getAppByName(accountId, HARNESS_SAMPLE_APP);
    if (existingApp == null) {
      return;
    }

    // Try cleanup app
    try {
      appService.delete(existingApp.getAppId());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, WingsException.USER)
          .addParam("reason", "Could not clean up harness sample application : " + e.getMessage());
    }
  }

  private SampleAppStatus isK8sApplicationHealthy(String accountId) {
    SampleAppStatus sampleAppStatus =
        SampleAppStatus.builder().deploymentType(DeploymentType.KUBERNETES.name()).build();
    List<SampleAppEntityStatus> entityStatusList = new ArrayList<>();
    Health health = Health.GOOD;

    // Verify Kubernetes app
    Application existingApp = appService.getAppByName(accountId, HARNESS_SAMPLE_APP);
    if (existingApp != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(HARNESS_SAMPLE_APP)
                               .entityType(EntityType.APPLICATION.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(HARNESS_SAMPLE_APP)
                               .entityType(EntityType.APPLICATION.name())
                               .health(Health.BAD)
                               .build());
      // App is bad, return
      sampleAppStatus.setStatusList(entityStatusList);
      sampleAppStatus.setHealth(Health.BAD);
      return sampleAppStatus;
    }
    String appId = existingApp.getAppId();

    // Verify service
    boolean isV2 = false;
    Service existingService = serviceResourceService.getServiceByName(appId, K8S_SERVICE_NAME);
    if (existingService != null) {
      isV2 = existingService.isK8sV2();
      if (!isV2) {
        // App is v1, set health as BAD
        sampleAppStatus.setStatusList(entityStatusList);
        sampleAppStatus.setHealth(Health.BAD);
        return sampleAppStatus;
      }
    }

    // Verify K8s Cloud provider
    SettingAttribute existingKubeCluster = settingsService.fetchSettingAttributeByName(
        accountId, K8S_CLOUD_PROVIDER_NAME, SettingVariableTypes.KUBERNETES_CLUSTER);
    if (existingKubeCluster != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_CLOUD_PROVIDER_NAME)
                               .entityType(Category.CLOUD_PROVIDER.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_CLOUD_PROVIDER_NAME)
                               .entityType(Category.CLOUD_PROVIDER.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify Docker connector
    SettingAttribute existingDockerConnector = settingsService.fetchSettingAttributeByName(
        accountId, HARNESS_DOCKER_HUB_CONNECTOR, SettingVariableTypes.DOCKER);
    if (existingDockerConnector != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(HARNESS_DOCKER_HUB_CONNECTOR)
                               .entityType(Category.CONNECTOR.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(HARNESS_DOCKER_HUB_CONNECTOR)
                               .entityType(Category.CONNECTOR.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify service
    if (existingService != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_SERVICE_NAME)
                               .entityType(EntityType.SERVICE.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_SERVICE_NAME)
                               .entityType(EntityType.SERVICE.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify artifact stream
    if (existingService != null) {
      ArtifactStream existingArtifactStream = artifactStreamService.getArtifactStreamByName(
          appId, existingService.getUuid(), DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME);
      if (existingArtifactStream != null) {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME)
                                 .entityType(EntityType.ARTIFACT_STREAM.name())
                                 .health(Health.GOOD)
                                 .build());
      } else {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME)
                                 .entityType(EntityType.ARTIFACT_STREAM.name())
                                 .health(Health.BAD)
                                 .build());
        health = Health.BAD;
      }
    }

    // Verify QA environment
    Environment existingQAEnvironment = environmentService.getEnvironmentByName(appId, K8S_QA_ENVIRONMENT);
    if (existingQAEnvironment != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_QA_ENVIRONMENT)
                               .entityType(EntityType.ENVIRONMENT.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_QA_ENVIRONMENT)
                               .entityType(EntityType.ENVIRONMENT.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify QA infra mapping
    if (existingService != null && existingQAEnvironment != null) {
      ServiceTemplate existingQAServiceTemplate =
          serviceTemplateService.get(appId, existingService.getUuid(), existingQAEnvironment.getUuid());
      if (existingQAServiceTemplate != null) {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.SERVICE_TEMPLATE.name())
                                 .health(Health.GOOD)
                                 .build());
      } else {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.SERVICE_TEMPLATE.name())
                                 .health(Health.BAD)
                                 .build());
        health = Health.BAD;
      }

      InfrastructureMapping existingQAInfraMapping = infrastructureMappingService.getInfraMappingByName(
          appId, existingQAEnvironment.getUuid(), K8S_SERVICE_INFRA_NAME);
      if (existingQAInfraMapping != null) {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.INFRASTRUCTURE_MAPPING.name())
                                 .health(Health.GOOD)
                                 .build());
      } else {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.INFRASTRUCTURE_MAPPING.name())
                                 .health(Health.BAD)
                                 .build());
        health = Health.BAD;
      }
    }

    // Verify Prod environment
    Environment existingProdEnvironment = environmentService.getEnvironmentByName(appId, K8S_PROD_ENVIRONMENT);
    if (existingProdEnvironment != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_PROD_ENVIRONMENT)
                               .entityType(EntityType.ENVIRONMENT.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_PROD_ENVIRONMENT)
                               .entityType(EntityType.ENVIRONMENT.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify prod infra mapping
    if (existingService != null && existingProdEnvironment != null) {
      ServiceTemplate existingProdServiceTemplate =
          serviceTemplateService.get(appId, existingService.getUuid(), existingProdEnvironment.getUuid());
      if (existingProdServiceTemplate != null) {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.SERVICE_TEMPLATE.name())
                                 .health(Health.GOOD)
                                 .build());
      } else {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.SERVICE_TEMPLATE.name())
                                 .health(Health.BAD)
                                 .build());
        health = Health.BAD;
      }

      InfrastructureMapping existingProdInfraMapping = infrastructureMappingService.getInfraMappingByName(
          appId, existingProdEnvironment.getUuid(), K8S_SERVICE_INFRA_NAME);
      if (existingProdInfraMapping != null) {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.INFRASTRUCTURE_MAPPING.name())
                                 .health(Health.GOOD)
                                 .build());
      } else {
        entityStatusList.add(SampleAppEntityStatus.builder()
                                 .entityName(K8S_SERVICE_INFRA_NAME)
                                 .entityType(EntityType.INFRASTRUCTURE_MAPPING.name())
                                 .health(Health.BAD)
                                 .build());
        health = Health.BAD;
      }
    }

    // Verify canary workflow
    Workflow existingCanaryWorkflow = workflowService.readWorkflowByName(appId, K8S_CANARY_WORKFLOW_NAME);
    if (existingCanaryWorkflow != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_CANARY_WORKFLOW_NAME)
                               .entityType(EntityType.WORKFLOW.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_CANARY_WORKFLOW_NAME)
                               .entityType(EntityType.WORKFLOW.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify rolling workflow
    Workflow existingRollingWorkflow = workflowService.readWorkflowByName(appId, K8S_ROLLING_WORKFLOW_NAME);
    if (existingRollingWorkflow != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_ROLLING_WORKFLOW_NAME)
                               .entityType(EntityType.WORKFLOW.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_ROLLING_WORKFLOW_NAME)
                               .entityType(EntityType.WORKFLOW.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }

    // Verify pipeline
    Pipeline existingPipeline = pipelineService.getPipelineByName(appId, K8S_PIPELINE_NAME);
    if (existingPipeline != null) {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_PIPELINE_NAME)
                               .entityType(EntityType.PIPELINE.name())
                               .health(Health.GOOD)
                               .build());
    } else {
      entityStatusList.add(SampleAppEntityStatus.builder()
                               .entityName(K8S_PIPELINE_NAME)
                               .entityType(EntityType.PIPELINE.name())
                               .health(Health.BAD)
                               .build());
      health = Health.BAD;
    }
    sampleAppStatus.setStatusList(entityStatusList);
    sampleAppStatus.setHealth(health);
    return sampleAppStatus;
  }
}
