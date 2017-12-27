package software.wings.service.impl.yaml;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.inject.Inject;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage.Level;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.command.CommandYaml;
import software.wings.yaml.workflow.WorkflowYaml;

public class YamlResourceServiceImpl implements YamlResourceService {
  @Inject private AppService appService;
  @Inject private YamlHistoryService yamlHistoryService;
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
  @Inject private YamlService yamlService;
  @Inject private SecretManager secretManager;

  private final Logger logger = LoggerFactory.getLogger(getClass());

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
    Validator.notNullCheck("No service command with the given id:" + serviceCommandId, serviceCommand);

    Command command = commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion());
    Validator.notNullCheck("No command with the given service command id:" + serviceCommandId, command);

    serviceCommand.setCommand(command);

    if (serviceCommand != null) {
      CommandYaml commandYaml =
          (CommandYaml) yamlHandlerFactory.getYamlHandler(YamlType.COMMAND, null).toYaml(serviceCommand, appId);
      return YamlHelper.getYamlRestResponse(yamlGitSyncService, serviceCommand.getUuid(), accountId, commandYaml,
          serviceCommand.getName() + YAML_EXTENSION);
    } else {
      // handle missing serviceCommand
      RestResponse rr = new RestResponse<>();
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR,
          "The ServiceCommand with appId: '" + appId + "' and serviceCommandId: '" + serviceCommandId
              + "' was not found!");
      return rr;
    }
  }

  /**
   * Update by app, service and service command ids and yaml payload
   *
   * @param accountId     the account id
   * @param yamlPayload the yaml version of the service command
   * @return the application
   */
  public RestResponse<ServiceCommand> updateServiceCommand(String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
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
        (Pipeline.Yaml) yamlHandlerFactory.getYamlHandler(YamlType.PIPELINE, null).toYaml(pipeline, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, pipeline.getUuid(), accountId, pipelineYaml, pipeline.getName() + YAML_EXTENSION);
  }

  /**
   * Update a pipeline that is sent as Yaml (in a JSON "wrapper")
   *
   *
   * @param accountId
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  public RestResponse<Pipeline> updatePipeline(String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
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
      RestResponse rr = new RestResponse<>();
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR,
          "The ArtifactStream with appId: '" + appId + "' and artifactStreamId: '" + artifactStreamId
              + "' was not found!");
      return rr;
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
        // handle service not found
        RestResponse rr = new RestResponse<>();
        YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR,
            "The Service with appId: '" + appId + "' and serviceId: '" + serviceId + "' was not found!");
        return rr;
      }
    }
    String payLoadName = artifactStream.getSourceName() + "(" + serviceName + ")";

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, artifactStream.getUuid(),
        appService.getAccountIdByAppId(appId), artifactStreamYaml, payLoadName + ".yaml");
  }

  /**
   * Update a trigger that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @param yamlPayload the yaml version of the service command
   * @param deleteEnabled required to allow deletions
   * @return the rest response
   */
  public RestResponse<ArtifactStream> updateTrigger(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled) {
    return null;
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

  public RestResponse<Workflow> updateWorkflow(String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  public RestResponse<YamlPayload> getSettingAttributesList(String accountId, String type) {
    // TODO

    return null;
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
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.ENVIRONMENT, null).toYaml(environment, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, environment.getUuid(), app.getAccountId(), yaml, environment.getName() + YAML_EXTENSION);
  }

  public RestResponse<YamlPayload> getService(String appId, String serviceId) {
    Application app = appService.get(appId);
    Service service = serviceResourceService.get(appId, serviceId, true);
    Validator.notNullCheck("Service is null for Id: " + serviceId, service);
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.SERVICE, null).toYaml(service, appId);
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
  public RestResponse<YamlPayload> getLambdaSpec(String accountId, String appId, String lambdaSpecId) {
    LambdaSpecification lambdaSpecification = serviceResourceService.getLambdaSpecificationById(appId, lambdaSpecId);

    BaseYaml yaml =
        yamlHandlerFactory.getYamlHandler(YamlType.DEPLOYMENT_SPECIFICATION, DeploymentType.AWS_LAMBDA.name())
            .toYaml(lambdaSpecification, appId);
    return YamlHelper.getYamlRestResponse(yamlGitSyncService, lambdaSpecification.getUuid(), accountId, yaml,
        YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getSettingAttribute(String accountId, String uuid) {
    SettingAttribute settingAttribute = settingsService.get(uuid);
    Validator.notNullCheck("SettingAttribute is not null for:" + uuid, settingAttribute);

    BaseYamlHandler yamlHandler = getSettingAttributeYamlHandler(settingAttribute);
    BaseYaml yaml = null;
    if (yamlHandler != null) {
      // TODO check if this is true
      yaml = yamlHandler.toYaml(settingAttribute, GLOBAL_APP_ID);
    }

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, settingAttribute.getUuid(), accountId, yaml,
        YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME + YAML_EXTENSION);
  }

  private BaseYamlHandler getSettingAttributeYamlHandler(SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();
    SettingVariableTypes settingVariableType = settingValue.getSettingType();

    switch (settingVariableType) {
      // cloud providers
      case AWS:
      case GCP:
      case PHYSICAL_DATA_CENTER:
        return yamlHandlerFactory.getYamlHandler(YamlType.CLOUD_PROVIDER, settingVariableType.name());

      // artifact servers - these don't have separate folders
      case JENKINS:
      case BAMBOO:
      case DOCKER:
      case NEXUS:
      case ARTIFACTORY:
      case ECR:
      case GCR:
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
      case DIRECT:
      case KUBERNETES:
        break;
      default:
        logger.warn("Unknown SettingVariable type:" + settingVariableType);
    }
    return null;
  }

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  public RestResponse<Environment> updateEnvironment(String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  public RestResponse<Service> updateService(String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  @Override
  public RestResponse<ConfigFile> updateConfigFile(String accountId, YamlPayload yamlPayload) {
    return yamlService.update(yamlPayload, accountId);
  }

  @Override
  public RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, String configFileUuid) {
    ConfigFile configFile = configService.get(appId, configFileUuid);
    if (configFile.getConfigOverrideType() != null) {
      return getConfigFileOverrideYaml(accountId, appId, configFile);
    } else {
      return getConfigFileYaml(accountId, appId, configFile);
    }
  }

  @Override
  public RestResponse<YamlPayload> getConfigFileYaml(String accountId, String appId, ConfigFile configFile) {
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.CONFIG_FILE, null).toYaml(configFile, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, configFile.getUuid(), accountId, yaml, configFile.getRelativeFilePath() + YAML_EXTENSION);
  }

  @Override
  public RestResponse<YamlPayload> getConfigFileOverrideYaml(String accountId, String appId, ConfigFile configFile) {
    BaseYaml yaml = yamlHandlerFactory.getYamlHandler(YamlType.CONFIG_FILE_OVERRIDE, null).toYaml(configFile, appId);
    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, configFile.getUuid(), accountId, yaml, configFile.getRelativeFilePath() + YAML_EXTENSION);
  }
}
