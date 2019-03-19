package software.wings.service.impl.yaml;

import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.INDEX_YAML;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE;
import static software.wings.beans.yaml.YamlType.APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.AppManifestType;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.common.Constants;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.setting.SettingValueYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfiguration.CVConfigurationYaml;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.command.CommandYaml;
import software.wings.yaml.workflow.WorkflowYaml;

import java.util.List;

@Singleton
public class YamlResourceServiceImpl implements YamlResourceService {
  @Inject private AppService appService;
  @Inject private CommandService commandService;
  @Inject private ConfigService configService;
  @Inject private EnvironmentService environmentService;
  @Inject private PipelineService pipelineService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private YamlArtifactStreamService yamlArtifactStreamService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private WorkflowService workflowService;
  @Inject private SettingsService settingsService;
  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private ApplicationManifestService applicationManifestService;

  private static final Logger logger = LoggerFactory.getLogger(YamlResourceServiceImpl.class);

  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @return the application
   */
  public RestResponse<YamlPayload> getServiceCommand(@NotEmpty String appId, @NotEmpty String serviceCommandId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);

    ServiceCommand serviceCommand = commandService.getServiceCommand(appId, serviceCommandId);
    if (serviceCommand != null) {
      Command command = commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion());
      Validator.notNullCheck("No command with the given service command id:" + serviceCommandId, command);

      serviceCommand.setCommand(command);

      CommandYaml commandYaml =
          (CommandYaml) yamlHandlerFactory.getYamlHandler(YamlType.COMMAND).toYaml(serviceCommand, appId);
      return YamlHelper.getYamlRestResponse(yamlGitSyncService, serviceCommand.getUuid(), accountId, commandYaml,
          serviceCommand.getName() + YAML_EXTENSION);
    } else {
      throw new WingsException(ErrorCode.GENERAL_YAML_ERROR, USER)
          .addParam("message",
              "The ServiceCommand with appId: '" + appId + "' and serviceCommandId: '" + serviceCommandId
                  + "' was not found!");
    }
  }

  /**
   * Gets the yaml version of a pipeline by pipelineId
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getPipeline(String appId, String pipelineId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, false);
    Validator.notNullCheck("No pipeline with the given id:" + pipelineId, pipeline);
    Pipeline.Yaml pipelineYaml =
        (Pipeline.Yaml) yamlHandlerFactory.getYamlHandler(YamlType.PIPELINE).toYaml(pipeline, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, pipeline.getUuid(), accountId, pipelineYaml, pipeline.getName() + YAML_EXTENSION);
  }

  public RestResponse<YamlPayload> getApplicationManifest(String appId, String applicationManifestId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);
    ApplicationManifest applicationManifest = applicationManifestService.getById(appId, applicationManifestId);

    Validator.notNullCheck("No Application Manifest with the given id:" + applicationManifestId, applicationManifest);
    ApplicationManifest.Yaml yaml =
        (ApplicationManifest.Yaml) yamlHandlerFactory.getYamlHandler(getYamlTypeFromAppManifest(applicationManifest))
            .toYaml(applicationManifest, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, applicationManifest.getUuid(), accountId, yaml, INDEX_YAML);
  }

  public RestResponse<YamlPayload> getManifestFile(String appId, String manifestFileId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);
    ManifestFile manifestFile = applicationManifestService.getManifestFileById(appId, manifestFileId);

    Validator.notNullCheck("No Manifest File with the given id:" + manifestFileId, manifestFile);
    return YamlHelper.getYamlRestResponseForActualFile(manifestFile.getFileContent(), manifestFile.getFileName());
  }

  @Override
  public RestResponse<YamlPayload> getNotificationGroup(String accountId, String notificationGroupId) {
    NotificationGroup notificationGroup =
        notificationSetupService.readNotificationGroup(accountId, notificationGroupId);
    Validator.notNullCheck("No notification group exists with the given id:" + notificationGroupId, notificationGroup);

    NotificationGroup.Yaml notificationGroupYaml =
        (NotificationGroup.Yaml) yamlHandlerFactory.getYamlHandler(YamlType.NOTIFICATION_GROUP)
            .toYaml(notificationGroup, GLOBAL_APP_ID);
    return YamlHelper.getYamlRestResponse(yamlGitSyncService, notificationGroup.getUuid(), accountId,
        notificationGroupYaml, notificationGroup.getName() + YAML_EXTENSION);
  }

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getTrigger(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);

    if (artifactStream == null) {
      // handle missing artifactStream
      throw new WingsException(ErrorCode.GENERAL_YAML_ERROR, USER)
          .addParam("message",
              "The ArtifactStream with appId: '" + appId + "' and artifactStreamId: '" + artifactStreamId
                  + "' was not found!");
    }

    ArtifactStream.Yaml artifactStreamYaml =
        yamlArtifactStreamService.getArtifactStreamYamlObject(appId, artifactStreamId);

    String serviceId = artifactStream.getServiceId();

    String serviceName = "";

    if (serviceId != null) {
      Service service = serviceResourceService.get(appId, serviceId);

      if (service != null) {
        serviceName = service.getName();
      } else {
        throw new WingsException(ErrorCode.GENERAL_YAML_ERROR, USER)
            .addParam(
                "message", "The Service with appId: '" + appId + "' and serviceId: '" + serviceId + "' was not found!");
      }
    }
    String payLoadName = artifactStream.getSourceName() + "(" + serviceName + ")";

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, artifactStream.getUuid(),
        appService.getAccountIdByAppId(appId), artifactStreamYaml, payLoadName + ".yaml");
  }

  /**
   * Gets the yaml for a workflow
   *
   * @param appId     the app id
   * @param workflowId the workflow id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getWorkflow(String appId, String workflowId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);
    Workflow workflow = workflowService.readWorkflow(appId, workflowId);
    WorkflowYaml workflowYaml = (WorkflowYaml) yamlHandlerFactory
                                    .getYamlHandler(YamlType.WORKFLOW,
                                        workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType().name())
                                    .toYaml(workflow, appId);

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, workflow.getUuid(), accountId, workflowYaml, workflow.getName() + YAML_EXTENSION);
  }

  /**
   * Gets the yaml for a ApplicationManifest
   *
   * @param appId     the app id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getApplicationManifest(String appId, ApplicationManifest manifest) {
    ApplicationManifest.Yaml yaml =
        (ApplicationManifest.Yaml) yamlHandlerFactory.getYamlHandler(getYamlTypeFromAppManifest(manifest))
            .toYaml(manifest, appId);

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, null, null, yaml, INDEX_YAML);
  }

  /**
   * Gets the yaml for a workflow
   *
   * @param appId     the app id
   * @param provisionerId the provisioner id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getProvisioner(String appId, String provisionerId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);
    InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerService.get(appId, provisionerId);
    InfrastructureProvisioner.Yaml provisionerYaml =
        (InfrastructureProvisioner.Yaml) yamlHandlerFactory
            .getYamlHandler(YamlType.PROVISIONER, infrastructureProvisioner.getInfrastructureProvisionerType())
            .toYaml(infrastructureProvisioner, appId);

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, infrastructureProvisioner.getUuid(), accountId,
        provisionerYaml, provisionerYaml.getName() + YAML_EXTENSION);
  }

  public RestResponse<YamlPayload> geCVConfiguration(String appId, String cvConfigId) {
    String accountId = appService.getAccountIdByAppId(appId);
    Validator.notNullCheck("No account found for appId:" + appId, accountId);

    CVConfiguration cvConfiguration = cvConfigurationService.getConfiguration(cvConfigId);
    CVConfigurationYaml cvConfigurationYaml =
        (CVConfigurationYaml) yamlHandlerFactory
            .getYamlHandler(YamlType.CV_CONFIGURATION, cvConfiguration.getStateType().name())
            .toYaml(cvConfiguration, appId);

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, cvConfiguration.getUuid(), accountId, cvConfigurationYaml,
        cvConfigurationYaml.getName() + YAML_EXTENSION);
  }

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  public RestResponse<YamlPayload> getGlobalSettingAttributesList(String accountId, String type) {
    List<SettingAttribute> settingAttributeList = settingsService.getSettingAttributesByType(accountId, type);
    BaseYaml yaml =
        yamlHandlerFactory.getYamlHandler(YamlType.ACCOUNT_DEFAULTS).toYaml(settingAttributeList, GLOBAL_APP_ID);
    return YamlHelper.getYamlRestResponse(yamlGitSyncService, null, accountId, yaml, DEFAULTS_YAML);
  }

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param appId   the app id
   * @param envId   the environment id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getEnvironment(String appId, String envId) {
    Application app = appService.get(appId);
    Environment environment = environmentService.get(appId, envId, true);
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.ENVIRONMENT).toYaml(environment, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, environment.getUuid(), app.getAccountId(), yaml, environment.getName() + YAML_EXTENSION);
  }

  public RestResponse<YamlPayload> getService(String appId, String serviceId) {
    Application app = appService.get(appId);
    Service service = serviceResourceService.get(appId, serviceId, true);
    Validator.notNullCheck("Service is null for Id: " + serviceId, service);
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.SERVICE).toYaml(service, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, service.getUuid(), app.getAccountId(), yaml, service.getName() + YAML_EXTENSION);
  }

  /**
   * Gets the yaml version of an environment by envId
   *
   * @param accountId the account id
   * @param appId   the app id
   * @param infraMappingId   infra mapping id
   * @return the rest response
   */
  @Override
  public RestResponse<YamlPayload> getInfraMapping(String accountId, String appId, String infraMappingId) {
    InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingId);

    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.INFRA_MAPPING, infraMapping.getInfraMappingType())
                        .toYaml(infraMapping, appId);

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, infraMapping.getUuid(), accountId, yaml, infraMapping.getName() + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getContainerTask(String accountId, String appId, String containerTaskId) {
    ContainerTask containerTask = serviceResourceService.getContainerTaskById(appId, containerTaskId);
    String yamlFileName;
    String yamlSubType;
    if (DeploymentType.ECS.name().equals(containerTask.getDeploymentType())) {
      yamlSubType = DeploymentType.ECS.name();
      yamlFileName = YamlConstants.ECS_CONTAINER_TASK_YAML_FILE_NAME;
    } else if (DeploymentType.KUBERNETES.name().equals(containerTask.getDeploymentType())) {
      yamlSubType = DeploymentType.KUBERNETES.name();
      yamlFileName = YamlConstants.KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME;
    } else {
      throw new WingsException("Unsupported deployment type: " + containerTask.getDeploymentType());
    }

    BaseYaml yaml =
        yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, yamlSubType).toYaml(containerTask, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, containerTask.getUuid(), accountId, yaml, yamlFileName + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getHelmChartSpecification(
      String accountId, String appId, String helmChartSpecificationId) {
    HelmChartSpecification helmChartSpecification =
        serviceResourceService.getHelmChartSpecificationById(appId, helmChartSpecificationId);
    String yamlFileName;
    yamlFileName = YamlConstants.HELM_CHART_YAML_FILE_NAME;

    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, DeploymentType.HELM.name())
                        .toYaml(helmChartSpecification, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, helmChartSpecification.getUuid(), accountId, yaml, yamlFileName + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getEcsServiceSpecification(
      String accountId, String appId, String ecsServiceSpecificationId) {
    EcsServiceSpecification ecsServiceSpecification =
        serviceResourceService.getEcsServiceSpecificationById(appId, ecsServiceSpecificationId);
    String yamlFileName = YamlConstants.ECS_SERVICE_SPEC_YAML_FILE_NAME;

    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, Constants.ECS_SERVICE_SPEC)
                        .toYaml(ecsServiceSpecification, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, ecsServiceSpecification.getUuid(), accountId, yaml, yamlFileName + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getPcfServiceSpecification(
      String accountId, String appId, String pcfServiceSpecificationId) {
    PcfServiceSpecification pcfServiceSpecification =
        serviceResourceService.getPcfServiceSpecificationById(appId, pcfServiceSpecificationId);
    String yamlFileName;
    yamlFileName = YamlConstants.PCF_MANIFEST_YAML_FILE_NAME;

    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, DeploymentType.PCF.name())
                        .toYaml(pcfServiceSpecification, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, pcfServiceSpecification.getUuid(), accountId, yaml, yamlFileName + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getLambdaSpec(String accountId, String appId, String lambdaSpecId) {
    LambdaSpecification lambdaSpecification = serviceResourceService.getLambdaSpecificationById(appId, lambdaSpecId);

    BaseYaml yaml =
        yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, DeploymentType.AWS_LAMBDA.name())
            .toYaml(lambdaSpecification, appId);
    return YamlHelper.getYamlRestResponse(yamlGitSyncService, lambdaSpecification.getUuid(), accountId, yaml,
        YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getUserDataSpec(String accountId, String appId, String userDataSpecId) {
    UserDataSpecification userDataSpecification =
        serviceResourceService.getUserDataSpecificationById(appId, userDataSpecId);

    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, DeploymentType.AMI.name())
                        .toYaml(userDataSpecification, appId);
    return YamlHelper.getYamlRestResponse(yamlGitSyncService, userDataSpecification.getUuid(), accountId, yaml,
        YamlConstants.USER_DATA_SPEC_YAML_FILE_NAME + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getSettingAttribute(String accountId, String uuid) {
    SettingAttribute settingAttribute = settingsService.get(uuid);
    Validator.notNullCheck("SettingAttribute is not null for:" + uuid, settingAttribute);

    SettingValueYamlHandler yamlHandler = getSettingValueYamlHandler(settingAttribute);
    BaseYaml yaml = null;
    if (yamlHandler != null) {
      // TODO check if this is true
      yaml = yamlHandler.toYaml(settingAttribute, GLOBAL_APP_ID);
    }

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, settingAttribute.getUuid(), accountId, yaml,
        YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getDefaultVariables(String accountId, String appId) {
    List<SettingAttribute> settingAttributeList =
        settingsService.getSettingAttributesByType(accountId, appId, GLOBAL_ENV_ID, SettingVariableTypes.STRING.name());

    YamlType yamlType = GLOBAL_APP_ID.equals(appId) ? YamlType.ACCOUNT_DEFAULTS : YamlType.APPLICATION_DEFAULTS;
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(yamlType).toYaml(settingAttributeList, appId);
    return YamlHelper.getYamlRestResponse(yamlGitSyncService, appId, accountId, yaml, DEFAULTS_YAML);
  }

  private SettingValueYamlHandler getSettingValueYamlHandler(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();
    SettingVariableTypes settingVariableType = settingValue.getSettingType();

    switch (settingVariableType) {
      // cloud providers
      case AWS:
      case GCP:
      case AZURE:
      case KUBERNETES_CLUSTER:
      case PHYSICAL_DATA_CENTER:
      case PCF:
        return yamlHandlerFactory.getYamlHandler(YamlType.CLOUD_PROVIDER, settingVariableType.name());

      // artifact servers - these don't have separate folders
      case JENKINS:
      case BAMBOO:
      case DOCKER:
      case NEXUS:
      case ARTIFACTORY:
      case ECR:
      case GCR:
      case ACR:
      case AMAZON_S3:
      case GIT:
        return yamlHandlerFactory.getYamlHandler(YamlType.ARTIFACT_SERVER, settingVariableType.name());

      // collaboration providers
      case SMTP:
      case SLACK:
        return yamlHandlerFactory.getYamlHandler(YamlType.COLLABORATION_PROVIDER, settingVariableType.name());

      // load balancers
      case ELB:
        return yamlHandlerFactory.getYamlHandler(YamlType.LOADBALANCER_PROVIDER, settingVariableType.name());

      // verification providers
      // JENKINS is also a (logical) part of this group
      case APP_DYNAMICS:
      case SPLUNK:
      case ELK:
      case LOGZ:
      case SUMO:
      case NEW_RELIC:
      case DYNA_TRACE:
      case PROMETHEUS:
        return yamlHandlerFactory.getYamlHandler(YamlType.VERIFICATION_PROVIDER, settingVariableType.name());

      case HOST_CONNECTION_ATTRIBUTES:
      case BASTION_HOST_CONNECTION_ATTRIBUTES:
        break;
      case KMS:
      case VAULT:
        break;
      case SERVICE_VARIABLE:
      case CONFIG_FILE:
      case SSH_SESSION_CONFIG:
      case YAML_GIT_SYNC:
      case KUBERNETES:
      case STRING:
        break;
      default:
        logger.warn("Unknown SettingVariable type:" + settingVariableType);
    }
    return null;
  }

  @Override
  public RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, String configFileUuid) {
    ConfigFile configFile = configService.get(appId, configFileUuid);

    return getConfigFileYaml(accountId, configFile);
  }

  private RestResponse<YamlPayload> getConfigFileYaml(String accountId, ConfigFile configFile) {
    String appId = configFile.getAppId();

    if (configFile.getConfigOverrideType() != null) {
      return getConfigFileOverrideYaml(accountId, appId, configFile);
    } else {
      return getConfigFileYaml(accountId, appId, configFile);
    }
  }

  @Override
  public RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, ConfigFile configFile) {
    ConfigFile.Yaml yaml =
        (ConfigFile.Yaml) yamlHandlerFactory.getYamlHandler(YamlType.CONFIG_FILE).toYaml(configFile, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, configFile.getUuid(), accountId, yaml, yaml.getFileName() + YAML_EXTENSION);
  }

  private RestResponse<YamlPayload> getConfigFileOverrideYaml(String accountId, String appId, ConfigFile configFile) {
    ConfigFile.OverrideYaml yaml =
        (ConfigFile.OverrideYaml) yamlHandlerFactory.getYamlHandler(YamlType.CONFIG_FILE_OVERRIDE)
            .toYaml(configFile, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, configFile.getUuid(), accountId, yaml, yaml.getFileName() + YAML_EXTENSION);
  }

  private RestResponse<YamlPayload> getAppManifestYaml(String accountId, ApplicationManifest applicationManifest) {
    YamlType yamlType = getYamlTypeFromAppManifest(applicationManifest);

    ApplicationManifest.Yaml yaml = (ApplicationManifest.Yaml) yamlHandlerFactory.getYamlHandler(yamlType).toYaml(
        applicationManifest, applicationManifest.getAppId());

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, applicationManifest.getUuid(), accountId, yaml,
        YamlConstants.APPLICATIONS_MANIFEST + YAML_EXTENSION);
  }

  private RestResponse<YamlPayload> getManifestFileYaml(String accountId, ManifestFile manifestFile) {
    ApplicationManifest applicationManifest =
        applicationManifestService.getById(manifestFile.getAppId(), manifestFile.getApplicationManifestId());
    YamlType yamlType = getYamlTypeFromAppManifest(applicationManifest);

    ManifestFile.Yaml yaml = (ManifestFile.Yaml) yamlHandlerFactory.getYamlHandler(yamlType).toYaml(
        applicationManifest, applicationManifest.getAppId());

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, applicationManifest.getUuid(), accountId, yaml,
        manifestFile.getFileName() + YAML_EXTENSION);
  }

  @Override
  public <T> RestResponse<YamlPayload> obtainEntityYamlVersion(String accountId, T entity) {
    if (entity instanceof ArtifactStream) {
      ArtifactStream artifactStream = (ArtifactStream) entity;
      return getTrigger(artifactStream.getAppId(), artifactStream.getUuid());
    } else if (entity instanceof ConfigFile) {
      return getConfigFileYaml(accountId, (ConfigFile) entity);
    } else if (entity instanceof SettingAttribute) {
      return getSettingAttribute(accountId, ((SettingAttribute) entity).getUuid());
    } else if (entity instanceof ServiceCommand) {
      ServiceCommand serviceCommand = (ServiceCommand) entity;
      return getServiceCommand(serviceCommand.getAppId(), serviceCommand.getUuid());
    } else if (entity instanceof ApplicationManifest) {
      return getAppManifestYaml(accountId, (ApplicationManifest) entity);
    } else if (entity instanceof ManifestFile) {
      return getManifestFileYaml(accountId, (ManifestFile) entity);
    }

    if (entity instanceof Base) {
      String appId = ((Base) entity).getAppId();
      String entityId = ((Base) entity).getUuid();
      // Validator.notNullCheck("No account found for appId:" + appId, accountId);

      entity = preProcessEntity(appId, entityId, entity);
      YamlType yamlType = yamlHandlerFactory.obtainEntityYamlType(entity);
      String entityName = yamlHandlerFactory.obtainEntityName(entity);
      String yamlHandlerSubType = yamlHandlerFactory.obtainYamlHandlerSubtype(entity);
      BaseYaml yaml = yamlHandlerFactory.getYamlHandler(yamlType, yamlHandlerSubType).toYaml(entity, appId);

      return YamlHelper.getYamlRestResponse(yamlGitSyncService, entityId, accountId, yaml, entityName + YAML_EXTENSION);
    }

    throw new InvalidRequestException(
        "Unhandled case while getting entity yaml version for entity type " + entity.getClass().getSimpleName());
  }

  private <T> T preProcessEntity(String appId, String entityId, T entity) {
    if (entity instanceof Environment) {
      return (T) environmentService.get(appId, entityId, true);
    } else if (entity instanceof Pipeline) {
      return (T) pipelineService.readPipeline(appId, entityId, false);
    } else if (entity instanceof Workflow) {
      return (T) workflowService.readWorkflow(appId, entityId);
    } else if (entity instanceof Service) {
      return (T) serviceResourceService.get(appId, entityId, true);
    }

    return entity;
  }

  @Override
  public YamlType getYamlTypeFromAppManifest(ApplicationManifest applicationManifest) {
    AppManifestType appManifestType = applicationManifestService.getAppManifestType(applicationManifest);

    switch (appManifestType) {
      case SERVICE:
        return APPLICATION_MANIFEST;
      case ENV:
        return APPLICATION_MANIFEST_VALUES_ENV_OVERRIDE;
      case ENV_SERVICE:
        return APPLICATION_MANIFEST_VALUES_ENV_SERVICE_OVERRIDE;
      default:
        unhandled(appManifestType);
        throw new WingsException("Unhandled app manifest type");
    }
  }
}
