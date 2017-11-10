package software.wings.service.impl.yaml;

import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;
import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.DockerStartCommandUnit;
import software.wings.beans.command.DockerStopCommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.PortCheckClearedCommandUnit;
import software.wings.beans.command.PortCheckListeningCommandUnit;
import software.wings.beans.command.ProcessCheckRunningCommandUnit;
import software.wings.beans.command.ProcessCheckStoppedCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
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
import software.wings.service.intfc.yaml.sync.YamlSyncService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Util;
import software.wings.utils.Validator;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.artifactstream.OrchestrationStreamActionYaml;
import software.wings.yaml.artifactstream.StreamActionYaml;
import software.wings.yaml.command.AwsLambdaCommandUnitYaml;
import software.wings.yaml.command.CodeDeployCommandUnitYaml;
import software.wings.yaml.command.CommandRefCommandUnitYaml;
import software.wings.yaml.command.CommandUnitYaml;
import software.wings.yaml.command.CopyConfigCommandUnitYaml;
import software.wings.yaml.command.DockerStartCommandUnitYaml;
import software.wings.yaml.command.DockerStopCommandUnitYaml;
import software.wings.yaml.command.ExecCommandUnitYaml;
import software.wings.yaml.command.KubernetesResizeCommandUnitYaml;
import software.wings.yaml.command.PortCheckClearedCommandUnitYaml;
import software.wings.yaml.command.PortCheckListenCommandUnitYaml;
import software.wings.yaml.command.ProcessCheckRunningCommandUnitYaml;
import software.wings.yaml.command.ProcessCheckStoppedCommandUnitYaml;
import software.wings.yaml.command.ResizeCommandUnitYaml;
import software.wings.yaml.command.ScpCommandUnitYaml;
import software.wings.yaml.command.ServiceCommandYaml;
import software.wings.yaml.command.SetupEnvCommandUnitYaml;
import software.wings.yaml.settingAttribute.AppDynamicsYaml;
import software.wings.yaml.settingAttribute.ArtifactoryYaml;
import software.wings.yaml.settingAttribute.AwsYaml;
import software.wings.yaml.settingAttribute.BambooYaml;
import software.wings.yaml.settingAttribute.DockerYaml;
import software.wings.yaml.settingAttribute.ElbYaml;
import software.wings.yaml.settingAttribute.ElkYaml;
import software.wings.yaml.settingAttribute.GcpYaml;
import software.wings.yaml.settingAttribute.HostConnectionAttributesYaml;
import software.wings.yaml.settingAttribute.JenkinsYaml;
import software.wings.yaml.settingAttribute.LogzYaml;
import software.wings.yaml.settingAttribute.NewRelicYaml;
import software.wings.yaml.settingAttribute.NexusYaml;
import software.wings.yaml.settingAttribute.PhysicalDataCenterYaml;
import software.wings.yaml.settingAttribute.SettingAttributeYaml;
import software.wings.yaml.settingAttribute.SlackYaml;
import software.wings.yaml.settingAttribute.SmtpYaml;
import software.wings.yaml.settingAttribute.SplunkYaml;
import software.wings.yaml.settingAttribute.SumoConfigYaml;
import software.wings.yaml.workflow.WorkflowYaml;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;

public class YamlResourceServiceImpl implements YamlResourceService {
  @Inject private AppService appService;
  @Inject private YamlHistoryService yamlHistoryService;
  @Inject private CommandService commandService;
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
  @Inject private YamlSyncService yamlSyncService;
  @Inject private SecretManager secretManager;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String ENV_ID_PROPERTY = "envId";

  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @return the application
   */
  public RestResponse<YamlPayload> getServiceCommand(@NotEmpty String appId, @NotEmpty String serviceCommandId) {
    Application app = appService.get(appId);
    ServiceCommandYaml serviceCommandYaml = new ServiceCommandYaml();

    List<Environment> environments = environmentService.getEnvByApp(appId);

    ServiceCommand serviceCommand = commandService.getServiceCommand(appId, serviceCommandId);

    if (serviceCommand != null) {
      serviceCommandYaml.setName(serviceCommand.getName());

      List<Command> commands = commandService.getCommandList(appId, serviceCommandId);

      if (commands != null && commands.size() > 0) {
        serviceCommandYaml.setCommandUnitType(commands.get(0).getCommandUnitType().getName());
        serviceCommandYaml.setCommandType(commands.get(0).getCommandType().toString());

        for (Command command : commands) {
          if (command.getVersion() != null && serviceCommand.getDefaultVersion() != null) {
            if (command.getVersion().intValue() == serviceCommand.getDefaultVersion().intValue()) {
              List<CommandUnit> commandUnits = command.getCommandUnits();

              if (commandUnits != null) {
                for (CommandUnit cu : commandUnits) {
                  CommandUnitYaml ycu;

                  CommandUnitType cut = cu.getCommandUnitType();

                  switch (cut) {
                    case EXEC:
                      ycu = new ExecCommandUnitYaml();
                      ((ExecCommandUnitYaml) ycu).setCommandPath(((ExecCommandUnit) cu).getCommandPath());
                      ((ExecCommandUnitYaml) ycu).setCommandString(((ExecCommandUnit) cu).getCommandString());
                      break;
                    case SCP:
                      ycu = new ScpCommandUnitYaml();
                      ((ScpCommandUnitYaml) ycu).setFileCategory(((ScpCommandUnit) cu).getFileCategory().getName());
                      ((ScpCommandUnitYaml) ycu)
                          .setDestinationDirectoryPath(((ScpCommandUnit) cu).getDestinationDirectoryPath());
                      break;
                    case COPY_CONFIGS:
                      ycu = new CopyConfigCommandUnitYaml();
                      ((CopyConfigCommandUnitYaml) ycu)
                          .setDestinationParentPath(((CopyConfigCommandUnit) cu).getDestinationParentPath());
                      break;
                    case COMMAND:
                      ycu = new CommandRefCommandUnitYaml();
                      ((CommandRefCommandUnitYaml) ycu).setReferenceId(((Command) cu).getReferenceId());
                      ((CommandRefCommandUnitYaml) ycu).setCommandType(((Command) cu).getCommandType().name());
                      break;
                    case SETUP_ENV:
                      ycu = new SetupEnvCommandUnitYaml();
                      ((SetupEnvCommandUnitYaml) ycu).setCommandString(((SetupEnvCommandUnit) cu).getCommandString());
                      break;
                    case DOCKER_START:
                      ycu = new DockerStartCommandUnitYaml();
                      ((DockerStartCommandUnitYaml) ycu)
                          .setCommandString(((DockerStartCommandUnit) cu).getCommandString());
                      break;
                    case DOCKER_STOP:
                      ycu = new DockerStopCommandUnitYaml();
                      ((DockerStopCommandUnitYaml) ycu)
                          .setCommandString(((DockerStopCommandUnit) cu).getCommandString());
                      break;
                    case PROCESS_CHECK_RUNNING:
                      ycu = new ProcessCheckRunningCommandUnitYaml();
                      ((ProcessCheckRunningCommandUnitYaml) ycu)
                          .setCommandString(((ProcessCheckRunningCommandUnit) cu).getCommandString());
                      break;
                    case PROCESS_CHECK_STOPPED:
                      ycu = new ProcessCheckStoppedCommandUnitYaml();
                      ((ProcessCheckStoppedCommandUnitYaml) ycu)
                          .setCommandString(((ProcessCheckStoppedCommandUnit) cu).getCommandString());
                      break;
                    case PORT_CHECK_CLEARED:
                      ycu = new PortCheckClearedCommandUnitYaml();
                      ((PortCheckClearedCommandUnitYaml) ycu)
                          .setCommandString(((PortCheckClearedCommandUnit) cu).getCommandString());
                      break;
                    case PORT_CHECK_LISTENING:
                      ycu = new PortCheckListenCommandUnitYaml();
                      ((PortCheckListenCommandUnitYaml) ycu)
                          .setCommandString(((PortCheckListeningCommandUnit) cu).getCommandString());
                      break;
                    case RESIZE:
                      ycu = new ResizeCommandUnitYaml();
                      break;
                    case CODE_DEPLOY:
                      ycu = new CodeDeployCommandUnitYaml();
                      break;
                    case AWS_LAMBDA:
                      ycu = new AwsLambdaCommandUnitYaml();
                      break;
                    case RESIZE_KUBERNETES:
                      ycu = new KubernetesResizeCommandUnitYaml();
                      break;
                    default:
                      // handle unfound
                      ycu = new CommandUnitYaml();
                  }

                  ycu.setName(cu.getName());
                  ycu.setCommandUnitType(cut.getName());

                  serviceCommandYaml.getCommandUnits().add(ycu);
                }
              }
            }
          }
        }
      }
    } else {
      // handle missing serviceCommand
      RestResponse rr = new RestResponse<>();
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
          "The ServiceCommand with appId: '" + appId + "' and serviceCommandId: '" + serviceCommandId
              + "' was not found!");
      return rr;
    }

    return YamlHelper.getYamlRestResponse(yamlGitSyncService, serviceCommand.getUuid(), app.getAccountId(),
        serviceCommandYaml, serviceCommand.getName() + ".yaml");
  }

  /**
   * Update by app, service and service command ids and yaml payload
   *
   * @param appId     the app id
   * @param serviceCommandId the service command id
   * @param yamlPayload the yaml version of the service command
   * @return the application
   */
  public RestResponse<ServiceCommand> updateServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceCommandId, YamlPayload yamlPayload, boolean deleteEnabled) {
    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    ServiceCommand serviceCommand = commandService.getServiceCommand(appId, serviceCommandId);

    // TODO - LEFT OFF HERE
    // Command command = commandService.getCommand()

    if (serviceCommand == null) {
      YamlHelper.addResponseMessage(
          rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "ServiceCommand not found!");
      return rr;
    }

    String yaml = yamlPayload.getYaml();

    if (yaml == null || yaml.isEmpty()) {
      YamlHelper.addMissingYamlMessage(rr);
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // get the before Yaml
    RestResponse beforeResponse = getServiceCommand(appId, serviceCommandId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (yaml.equals(beforeYaml)) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    ServiceCommandYaml serviceCommandYaml = null;

    try {
      serviceCommandYaml = mapper.readValue(yaml, ServiceCommandYaml.class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (serviceCommandYaml == null) {
      // handle missing or unmappable serviceCommandYaml
      return rr;
    }

    /* REF
    name: Install
    commandUnitType: Command
    commandType: INSTALL
    */

    /*
    serviceCommand.setName(serviceCommandYaml.getName());
    serviceCommand.set(serviceCommandYaml.getName());
    serviceCommand.setName(serviceCommandYaml.getName());
    */

    List<CommandUnitYaml> commandUnitYamls = serviceCommandYaml.getCommandUnits();

    for (CommandUnitYaml cuy : commandUnitYamls) {
      CommandUnitType cut = CommandUnitType.valueOf(cuy.getCommandUnitType());
    }

    //-------------------
    ServiceCommandYaml beforeServiceCommandYaml = null;

    /*
    List<Command> commands = commandService.getCommandList(appId, serviceCommandId);

    if (commands != null && commands.size() > 0) {
      for (Command command : commands) {

        if (command.getVersion() != null && serviceCommand.getDefaultVersion() != null) {
          if (command.getVersion().intValue() == serviceCommand.getDefaultVersion().intValue()) {
            List<CommandUnit> commandUnits = command.getCommandUnits();

            if (commandUnits != null) {
              for (CommandUnit cu : commandUnits) {
                CommandUnitYaml ycu;

                CommandUnitType cut = cu.getCommandUnitType();

                switch (cut) {
                  case EXEC:
                    ycu = new ExecCommandUnitYaml();
                    ((ExecCommandUnitYaml) ycu).setCommandPath(((ExecCommandUnit) cu).getCommandPath());
                    ((ExecCommandUnitYaml) ycu).setCommandString(((ExecCommandUnit) cu).getCommandString());
                    break;
                  case SCP:
                    ycu = new ScpCommandUnitYaml();
                    ((ScpCommandUnitYaml) ycu).setFileCategory(((ScpCommandUnit) cu).getFileCategory().getName());
                    ((ScpCommandUnitYaml) ycu).setDestinationDirectoryPath(((ScpCommandUnit)
    cu).getDestinationDirectoryPath()); break; case COPY_CONFIGS: ycu = new CopyConfigCommandUnitYaml();
                    ((CopyConfigCommandUnitYaml) ycu).setDestinationParentPath(((CopyConfigCommandUnit)
    cu).getDestinationParentPath()); break; case COMMAND: ycu = new CommandRefCommandUnitYaml();
                    ((CommandRefCommandUnitYaml) ycu).setReferenceId(((Command) cu).getReferenceId());
                    ((CommandRefCommandUnitYaml) ycu).setCommandType(((Command) cu).getCommandType().name());
                    break;
                  case SETUP_ENV:
                    ycu = new SetupEnvCommandUnitYaml();
                    ((SetupEnvCommandUnitYaml) ycu).setCommandString(((SetupEnvCommandUnit) cu).getCommandString());
                    break;
                  case DOCKER_START:
                    ycu = new DockerStartCommandUnitYaml();
                    ((DockerStartCommandUnitYaml) ycu).setCommandString(((DockerStartCommandUnit)
    cu).getCommandString()); break; case DOCKER_STOP: ycu = new DockerStopCommandUnitYaml();
                    ((DockerStopCommandUnitYaml) ycu).setCommandString(((DockerStopCommandUnit) cu).getCommandString());
                    break;
                  case PROCESS_CHECK_RUNNING:
                    ycu = new ProcessCheckRunningCommandUnitYaml();
                    ((ProcessCheckRunningCommandUnitYaml) ycu).setCommandString(((ProcessCheckRunningCommandUnit)
    cu).getCommandString()); break; case PROCESS_CHECK_STOPPED: ycu = new ProcessCheckStoppedCommandUnitYaml();
                    ((ProcessCheckStoppedCommandUnitYaml) ycu).setCommandString(((ProcessCheckStoppedCommandUnit)
    cu).getCommandString()); break; case PORT_CHECK_CLEARED: ycu = new PortCheckClearedCommandUnitYaml();
                    ((PortCheckClearedCommandUnitYaml) ycu).setCommandString(((PortCheckClearedCommandUnit)
    cu).getCommandString()); break; case PORT_CHECK_LISTENING: ycu = new PortCheckListenCommandUnitYaml();
                    ((PortCheckListenCommandUnitYaml) ycu).setCommandString(((PortCheckListeningCommandUnit)
    cu).getCommandString()); break; case RESIZE: ycu = new ResizeCommandUnitYaml(); break; case CODE_DEPLOY: ycu = new
    CodeDeployCommandUnitYaml(); break; case AWS_LAMBDA: ycu = new AwsLambdaCommandUnitYaml(); break; case
    RESIZE_KUBERNETES: ycu = new KubernetesResizeCommandUnitYaml(); break; default:
                    // handle unfound
                    ycu = new CommandUnitYaml();
                }

              }
            }
          }
        }

      }
    }
    */

    /*
    SettingVariableTypes settingVariableType = SettingVariableTypes.valueOf(serviceCommand.getValue().getType());

    logger.info("*********** settingVariableType: " + settingVariableType);

    if (settingVariableType == null) {
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "Unrecognized
    settingVariableType: '" + settingVariableType + "'."); return rr;
    }

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      SettingValue beforeConfig;
      SettingValue config;

    }
    */

    return rr;
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
    return yamlSyncService.update(yamlPayload, accountId);
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
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
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
        YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
            "The Service with appId: '" + appId + "' and serviceId: '" + serviceId + "' was not found!");
        return rr;
      }
    }

    List<ArtifactStreamAction> streamActions = artifactStream.getStreamActions();

    if (streamActions != null) {
      for (ArtifactStreamAction sa : streamActions) {
        StreamActionYaml say;

        switch (sa.getWorkflowType()) {
          case ORCHESTRATION:
            say = new OrchestrationStreamActionYaml();
            say.setWorkflowName(sa.getWorkflowName());
            say.setWorkflowType(sa.getWorkflowType().name());
            say.setEnvName(sa.getEnvName());
            break;
          case PIPELINE:
            say = new StreamActionYaml();
            say.setWorkflowName(sa.getWorkflowName());
            say.setWorkflowType(sa.getWorkflowType().name());
            break;
          case SIMPLE:
            say = new StreamActionYaml();
            break;
          default:
            // handle not found
            say = new StreamActionYaml();
        }

        // TODO Rishi mentioned streamactions / triggers would be pulled out of artifact streams. Skipping the handling
        // right now
        //        artifactStreamYaml.getStreamActions().add(say);
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
    return yamlSyncService.update(yamlPayload, accountId);
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
   * Gets the yaml for a setting attribute by accountId and uuid
   *
   * @param accountId the account id
   * @param uuid      the uid of the setting attribute
   * @return the rest response
   */
  public RestResponse<YamlPayload> getSettingAttribute(String accountId, String uuid) {
    SettingAttribute settingAttribute = settingsService.get(uuid);

    if (settingAttribute == null) {
      RestResponse rr = new RestResponse<>();
      YamlHelper.addSettingAttributeNotFoundMessage(rr, uuid);
      return rr;
    }

    SettingVariableTypes settingVariableType = SettingVariableTypes.valueOf(settingAttribute.getValue().getType());

    SettingAttributeYaml settingAttributeYaml;

    switch (settingVariableType) {
      // cloud providers
      case AWS:
        AwsYaml awsYaml = new AwsYaml(settingAttribute);
        AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();

        try {
          String secretKey = secretManager.getEncryptedYamlRef(awsConfig);
          awsYaml.setSecretKey(secretKey);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid Secret key. Should be a valid url to a secret");
          throw new WingsException(e);
        }
        settingAttributeYaml = awsYaml;
        break;

      case GCP:
        GcpYaml gcpYaml = new GcpYaml(settingAttribute);
        GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();

        try {
          String fileContent = secretManager.getEncryptedYamlRef(gcpConfig);
          gcpYaml.setServiceAccountKeyFileContent(fileContent);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid File Content. Should be a valid url to a secret");
          throw new WingsException(e);
        }
        settingAttributeYaml = gcpYaml;
        break;

      case PHYSICAL_DATA_CENTER:
        settingAttributeYaml = new PhysicalDataCenterYaml(settingAttribute);
        break;

      // artifact servers
      case JENKINS:
        JenkinsYaml jenkinsYaml = new JenkinsYaml(settingAttribute);

        JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(jenkinsConfig);
          jenkinsYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
          throw new WingsException(e);
        }
        settingAttributeYaml = jenkinsYaml;
        break;

      case BAMBOO:
        BambooYaml bambooYaml = new BambooYaml(settingAttribute);
        BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(bambooConfig);
          bambooYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
          throw new WingsException(e);
        }
        settingAttributeYaml = bambooYaml;
        break;

      case DOCKER:

        DockerYaml dockerYaml = new DockerYaml(settingAttribute);
        DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(dockerConfig);
          dockerYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
          throw new WingsException(e);
        }
        settingAttributeYaml = dockerYaml;
        break;

      case NEXUS:
        NexusYaml nexusYaml = new NexusYaml(settingAttribute);
        NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(nexusConfig);
          nexusYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = nexusYaml;
        break;
      case ARTIFACTORY:
        ArtifactoryYaml artifactoryYaml = new ArtifactoryYaml(settingAttribute);
        ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(artifactoryConfig);
          artifactoryYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = artifactoryYaml;
        break;

      // collaboration providers
      case SMTP:
        SmtpYaml smtpYaml = new SmtpYaml(settingAttribute);
        SmtpConfig smtpConfig = (SmtpConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(smtpConfig);
          smtpYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = smtpYaml;
        break;

      case SLACK:
        settingAttributeYaml = new SlackYaml(settingAttribute);
        break;

      // load balancers
      case ELB:
        ElbYaml elbYaml = new ElbYaml(settingAttribute);
        ElasticLoadBalancerConfig elbConfig = (ElasticLoadBalancerConfig) settingAttribute.getValue();

        try {
          String secretKey = secretManager.getEncryptedYamlRef(elbConfig);
          elbYaml.setSecretKey(secretKey);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = elbYaml;
        break;

      // verification providers
      // JENKINS is also a (logical) part of this group
      case APP_DYNAMICS:
        AppDynamicsYaml appDynamicsYaml = new AppDynamicsYaml(settingAttribute);
        AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(appDynamicsConfig);
          appDynamicsYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = appDynamicsYaml;
        break;

      case NEW_RELIC:
        NewRelicYaml newRelicYaml = new NewRelicYaml(settingAttribute);
        NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

        try {
          String apiKey = secretManager.getEncryptedYamlRef(newRelicConfig);
          newRelicYaml.setApiKey(apiKey);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid api key. Should be a valid url to a secret");
        }
        settingAttributeYaml = newRelicYaml;
        break;

      case SUMO:
        SumoConfigYaml sumoConfigYaml = new SumoConfigYaml(settingAttribute);
        SumoConfig sumoConfig = (SumoConfig) settingAttribute.getValue();

        try {
          String accessId = secretManager.getEncryptedYamlRef(sumoConfig, "accessId");
          sumoConfigYaml.setAccessId(accessId);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid access id. Should be a valid url to a secret");
        }

        try {
          String accessKey = secretManager.getEncryptedYamlRef(sumoConfig, "accessKey");
          sumoConfigYaml.setAccessKey(accessKey);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid access key. Should be a valid url to a secret");
        }
        settingAttributeYaml = sumoConfigYaml;
        break;

      case SPLUNK:
        SplunkYaml splunkYaml = new SplunkYaml(settingAttribute);
        SplunkConfig splunkConfig = (SplunkConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(splunkConfig);
          splunkYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = splunkYaml;
        break;

      case ELK:
        ElkYaml elkYaml = new ElkYaml(settingAttribute);
        ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();

        try {
          String password = secretManager.getEncryptedYamlRef(elkConfig);
          elkYaml.setPassword(password);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid password. Should be a valid url to a secret");
        }
        settingAttributeYaml = elkYaml;
        break;

      case LOGZ:
        LogzYaml logzYaml = new LogzYaml(settingAttribute);
        LogzConfig logzConfig = (LogzConfig) settingAttribute.getValue();

        try {
          String token = secretManager.getEncryptedYamlRef(logzConfig);
          logzYaml.setToken(token);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid token. Should be a valid url to a secret");
        }
        settingAttributeYaml = logzYaml;
        break;

      case HOST_CONNECTION_ATTRIBUTES:
        HostConnectionAttributesYaml yaml = new HostConnectionAttributesYaml(settingAttribute);
        HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) settingAttribute.getValue();

        try {
          String key = secretManager.getEncryptedYamlRef(hostConnectionAttributes);
          yaml.setKey(key);
        } catch (IllegalAccessException e) {
          logger.warn("Invalid key. Should be a valid url to a secret");
        }
        settingAttributeYaml = yaml;
        break;

      default:
        // handle not found
        RestResponse rr = new RestResponse<>();
        YamlHelper.addUnknownSettingVariableTypeMessage(rr, settingVariableType);
        return rr;
    }

    if (settingAttributeYaml != null) {
      return YamlHelper.getYamlRestResponse(yamlGitSyncService, settingAttribute.getUuid(), accountId,
          settingAttributeYaml, settingAttribute.getName() + YAML_EXTENSION);
    }

    return null;
  }

  /**
   * Update setting attribute sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId   the account id
   * @param uuid        the uid of the setting attribute
   * @param type        the SettingVariableTypes
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  public RestResponse<SettingAttribute> updateSettingAttribute(
      String accountId, String uuid, String type, YamlPayload yamlPayload, boolean deleteEnabled) {
    logger.info("*********** type: " + type);

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    SettingAttribute settingAttribute = settingsService.get(uuid);

    if (settingAttribute == null) {
      YamlHelper.addSettingAttributeNotFoundMessage(rr, uuid);
      return rr;
    }

    String yaml = yamlPayload.getYaml();

    if (yaml == null || yaml.isEmpty()) {
      YamlHelper.addMissingYamlMessage(rr);
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // get the before Yaml
    RestResponse beforeResponse = getSettingAttribute(accountId, uuid);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (yaml.equals(beforeYaml)) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    //-------------------

    SettingVariableTypes settingVariableType = SettingVariableTypes.valueOf(settingAttribute.getValue().getType());

    logger.info("*********** settingVariableType: " + settingVariableType);

    if (settingVariableType == null) {
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
          "Unrecognized settingVariableType: '" + settingVariableType + "'.");
      return rr;
    }

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      SettingValue beforeConfig;
      SettingValue config;

      // TODO - these can probably be refactored (quite a bit)

      try {
        switch (settingVariableType) {
          // cloud providers
          case AWS:
            AwsYaml beforeAwsYaml = mapper.readValue(beforeYaml, AwsYaml.class);
            if (beforeAwsYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeAwsYaml could not be correctly mapped.");
              return rr;
            }

            AwsYaml awsYaml = mapper.readValue(yaml, AwsYaml.class);
            if (awsYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "awsYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(awsYaml.getName());
            config = AwsConfig.builder()
                         .accountId(accountId)
                         .accessKey(awsYaml.getAccessKey())
                         .secretKey(null)
                         .encryptedSecretKey(awsYaml.getSecretKey())
                         .build();
            settingAttribute.setValue(config);
            break;
          case GCP:
            GcpYaml beforeGcpYaml = mapper.readValue(beforeYaml, GcpYaml.class);
            if (beforeGcpYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeGcpYaml could not be correctly mapped.");
              return rr;
            }

            GcpYaml gcpYaml = mapper.readValue(yaml, GcpYaml.class);
            if (gcpYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "gcpYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(gcpYaml.getName());
            config = GcpConfig.builder()
                         .serviceAccountKeyFileContent(null)
                         .encryptedServiceAccountKeyFileContent(gcpYaml.getServiceAccountKeyFileContent())
                         .build();
            settingAttribute.setValue(config);
            break;

          case PHYSICAL_DATA_CENTER:
            PhysicalDataCenterYaml beforePdcYaml = mapper.readValue(beforeYaml, PhysicalDataCenterYaml.class);
            if (beforePdcYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforePdcYaml could not be correctly mapped.");
              return rr;
            }

            PhysicalDataCenterYaml pdcYaml = mapper.readValue(yaml, PhysicalDataCenterYaml.class);
            if (pdcYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "pdcYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(pdcYaml.getName());
            break;

          // artifact servers
          case JENKINS:
            JenkinsYaml beforeJenkinsYaml = mapper.readValue(beforeYaml, JenkinsYaml.class);
            if (beforeJenkinsYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeJenkinsYaml could not be correctly mapped.");
              return rr;
            }

            JenkinsYaml jenkinsYaml = mapper.readValue(yaml, JenkinsYaml.class);
            if (jenkinsYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "jenkinsYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(jenkinsYaml.getName());
            config = JenkinsConfig.builder()
                         .accountId(accountId)
                         .jenkinsUrl(jenkinsYaml.getUrl())
                         .password(jenkinsYaml.getPassword().toCharArray())
                         .encryptedPassword(jenkinsYaml.getPassword())
                         .username(jenkinsYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          case BAMBOO:
            BambooYaml beforeBambooYaml = mapper.readValue(beforeYaml, BambooYaml.class);
            if (beforeBambooYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeBambooYaml could not be correctly mapped.");
              return rr;
            }

            BambooYaml bambooYaml = mapper.readValue(yaml, BambooYaml.class);
            if (bambooYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "bambooYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(bambooYaml.getName());
            config = BambooConfig.builder()
                         .accountId(accountId)
                         .bambooUrl(bambooYaml.getUrl())
                         .password(null)
                         .encryptedPassword(bambooYaml.getPassword())
                         .username(bambooYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          case DOCKER:
            DockerYaml beforeDockerYaml = mapper.readValue(beforeYaml, DockerYaml.class);
            if (beforeDockerYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeDockerYaml could not be correctly mapped.");
              return rr;
            }

            DockerYaml dockerYaml = mapper.readValue(yaml, DockerYaml.class);
            if (dockerYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "dockerYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(dockerYaml.getName());
            config = DockerConfig.builder()
                         .accountId(accountId)
                         .dockerRegistryUrl(dockerYaml.getUrl())
                         .password(null)
                         .encryptedPassword(dockerYaml.getPassword())
                         .username(dockerYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          case NEXUS:
            NexusYaml beforeNexusYaml = mapper.readValue(beforeYaml, NexusYaml.class);
            if (beforeNexusYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeNexusYaml could not be correctly mapped.");
              return rr;
            }

            NexusYaml nexusYaml = mapper.readValue(yaml, NexusYaml.class);
            if (nexusYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "nexusYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(nexusYaml.getName());
            config = NexusConfig.builder()
                         .accountId(accountId)
                         .nexusUrl(nexusYaml.getUrl())
                         .password(null)
                         .encryptedPassword(nexusYaml.getPassword())
                         .username(nexusYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          case ARTIFACTORY:
            ArtifactoryYaml beforeArtifactoryYaml = mapper.readValue(beforeYaml, ArtifactoryYaml.class);
            if (beforeArtifactoryYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeArtifactoryYaml could not be correctly mapped.");
              return rr;
            }

            ArtifactoryYaml artifactoryYaml = mapper.readValue(yaml, ArtifactoryYaml.class);
            if (artifactoryYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "artifactoryYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(artifactoryYaml.getName());
            config = ArtifactoryConfig.builder()
                         .accountId(accountId)
                         .artifactoryUrl(artifactoryYaml.getUrl())
                         .password(null)
                         .encryptedPassword(artifactoryYaml.getPassword())
                         .username(artifactoryYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          // collaboration providers
          case SMTP:
            SmtpYaml beforeSmtpYaml = mapper.readValue(beforeYaml, SmtpYaml.class);
            if (beforeSmtpYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeSmtpYaml could not be correctly mapped.");
              return rr;
            }

            SmtpYaml smtpYaml = mapper.readValue(yaml, SmtpYaml.class);
            if (smtpYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "smtpYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(smtpYaml.getName());
            config = SmtpConfig.builder()
                         .accountId(accountId)
                         .fromAddress(smtpYaml.getFromAddress())
                         .host(smtpYaml.getHost())
                         .password(null)
                         .encryptedPassword(smtpYaml.getPassword())
                         .port(smtpYaml.getPort())
                         .username(smtpYaml.getUsername())
                         .useSSL(smtpYaml.isUseSSL())
                         .build();
            settingAttribute.setValue(config);
            break;

          case SLACK:
            SlackYaml beforeSlackYaml = mapper.readValue(beforeYaml, SlackYaml.class);
            if (beforeSlackYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeSlackYaml could not be correctly mapped.");
              return rr;
            }

            SlackYaml slackYaml = mapper.readValue(yaml, SlackYaml.class);
            if (slackYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "slackYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(slackYaml.getName());
            config =
                SlackConfig.Builder.aSlackConfig().withOutgoingWebhookUrl(slackYaml.getOutgoingWebhookUrl()).build();
            settingAttribute.setValue(config);
            break;

          // load balancers
          case ELB:
            ElbYaml beforeElbYaml = mapper.readValue(beforeYaml, ElbYaml.class);
            if (beforeElbYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeElbYaml could not be correctly mapped.");
              return rr;
            }

            ElbYaml elbYaml = mapper.readValue(yaml, ElbYaml.class);
            if (elbYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "elbYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(elbYaml.getName());
            config = ElasticLoadBalancerConfig.builder()
                         .accountId(accountId)
                         .accessKey(elbYaml.getAccessKey())
                         .loadBalancerName(elbYaml.getLoadBalancerName())
                         .secretKey(null)
                         .encryptedSecretKey(elbYaml.getSecretKey())
                         .build();
            settingAttribute.setValue(config);
            break;

          // verification providers
          // JENKINS is also a (logical) part of this group
          case APP_DYNAMICS:
            AppDynamicsYaml beforeAppDynamicsYaml = mapper.readValue(beforeYaml, AppDynamicsYaml.class);
            if (beforeAppDynamicsYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeAppDynamicsYaml could not be correctly mapped.");
              return rr;
            }

            AppDynamicsYaml appDynamicsYaml = mapper.readValue(yaml, AppDynamicsYaml.class);
            if (appDynamicsYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "appDynamicsYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(appDynamicsYaml.getName());
            config = AppDynamicsConfig.builder()
                         .accountId(accountId)
                         .accountname(appDynamicsYaml.getAccountname())
                         .controllerUrl(appDynamicsYaml.getUrl())
                         .password(null)
                         .encryptedPassword(appDynamicsYaml.getPassword())
                         .username(appDynamicsYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          case NEW_RELIC:
            NewRelicYaml beforeNewRelicYaml = mapper.readValue(beforeYaml, NewRelicYaml.class);
            if (beforeNewRelicYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeNewRelicYaml could not be correctly mapped.");
              return rr;
            }

            NewRelicYaml newRelicYaml = mapper.readValue(yaml, NewRelicYaml.class);
            if (newRelicYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "newRelicYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(newRelicYaml.getName());
            config = NewRelicConfig.builder()
                         .accountId(accountId)
                         .apiKey(null)
                         .encryptedApiKey(newRelicYaml.getApiKey())
                         .build();
            settingAttribute.setValue(config);
            break;

          case SUMO:
            SumoConfigYaml beforeSumoConfigYaml = mapper.readValue(beforeYaml, SumoConfigYaml.class);
            if (beforeSumoConfigYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeSumoConfigYaml could not be correctly mapped.");
              return rr;
            }

            SumoConfigYaml sumoConfigYaml = mapper.readValue(yaml, SumoConfigYaml.class);
            if (sumoConfigYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "sumoConfigYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(sumoConfigYaml.getName());

            SumoConfig sumoConfig = new SumoConfig();
            sumoConfig.setAccessId(null);
            sumoConfig.setEncryptedAccessId(sumoConfigYaml.getAccessId());
            sumoConfig.setAccessKey(null);
            sumoConfig.setEncryptedAccessKey(sumoConfigYaml.getAccessKey());
            sumoConfig.setAccountId(accountId);
            sumoConfig.setSumoUrl(sumoConfigYaml.getSumoUrl());

            settingAttribute.setValue(sumoConfig);
            break;

          case SPLUNK:
            SplunkYaml beforeSplunkYaml = mapper.readValue(beforeYaml, SplunkYaml.class);
            if (beforeSplunkYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeSplunkYaml could not be correctly mapped.");
              return rr;
            }

            SplunkYaml splunkYaml = mapper.readValue(yaml, SplunkYaml.class);
            if (splunkYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "splunkYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(splunkYaml.getName());
            config = SplunkConfig.builder()
                         .accountId(accountId)
                         .password(null)
                         .encryptedPassword(splunkYaml.getPassword())
                         .splunkUrl(splunkYaml.getUrl())
                         .username(splunkYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
            break;

          case ELK:
            ElkYaml beforeElkYaml = mapper.readValue(beforeYaml, ElkYaml.class);
            if (beforeElkYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeElkYaml could not be correctly mapped.");
              return rr;
            }

            ElkYaml elkYaml = mapper.readValue(yaml, ElkYaml.class);
            if (elkYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "elkYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(elkYaml.getName());

            ElkConfig elkConfig = new ElkConfig();
            elkConfig.setAccountId(accountId);
            elkConfig.setElkUrl(elkYaml.getUrl());
            elkConfig.setPassword(null);
            elkConfig.setEncryptedPassword(elkYaml.getPassword());
            elkConfig.setUsername(elkYaml.getUsername());
            settingAttribute.setValue(elkConfig);
            break;

          case LOGZ:
            LogzYaml beforeLogzYaml = mapper.readValue(beforeYaml, LogzYaml.class);
            if (beforeLogzYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeLogzYaml could not be correctly mapped.");
              return rr;
            }

            LogzYaml logzYaml = mapper.readValue(yaml, LogzYaml.class);
            if (logzYaml == null) {
              YamlHelper.addResponseMessage(
                  rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "logzYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(logzYaml.getName());
            LogzConfig logzConfig = new LogzConfig();
            logzConfig.setAccountId(accountId);
            logzConfig.setLogzUrl(logzYaml.getUrl());
            logzConfig.setToken(null);
            logzConfig.setEncryptedToken(logzYaml.getToken());
            settingAttribute.setValue(logzConfig);
            break;

          case HOST_CONNECTION_ATTRIBUTES:
            HostConnectionAttributesYaml beforeHostConnAttrYaml =
                mapper.readValue(beforeYaml, HostConnectionAttributesYaml.class);
            if (beforeHostConnAttrYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "beforeHostConnAttrYaml could not be correctly mapped.");
              return rr;
            }

            HostConnectionAttributesYaml hostConnAttrYaml = mapper.readValue(yaml, HostConnectionAttributesYaml.class);
            if (hostConnAttrYaml == null) {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO,
                  "hostConnAttrYaml could not be correctly mapped.");
              return rr;
            }

            settingAttribute.setName(hostConnAttrYaml.getName());

            AccessType accessType = Util.getEnumFromString(AccessType.class, hostConnAttrYaml.getAccessType());
            ConnectionType connectionType =
                Util.getEnumFromString(ConnectionType.class, hostConnAttrYaml.getConnectionType());
            config = HostConnectionAttributes.Builder.aHostConnectionAttributes()
                         .withAccessType(accessType)
                         .withAccountId(accountId)
                         .withConnectionType(connectionType)
                         .withKey(null)
                         .withEncyptedKey(hostConnAttrYaml.getKey())
                         .withUserName(hostConnAttrYaml.getUserName())
                         .build();
            settingAttribute.setValue(config);
            break;

          default:
            // handle not found
            YamlHelper.addUnknownSettingVariableTypeMessage(rr, settingVariableType);
            return rr;
        }
      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        // bad before Yaml
        e.printStackTrace();
        YamlHelper.addCouldNotMapBeforeYamlMessage(rr);
        return rr;
      }
    } else {
      // missing before Yaml
      YamlHelper.addMissingBeforeYamlMessage(rr);
      return rr;
    }
    //-------------------

    settingAttribute = settingsService.update(settingAttribute);

    // return the new resource
    if (settingAttribute != null) {
      // save the before yaml version
      YamlVersion beforeYamLVersion = aYamlVersion()
                                          .withAccountId(settingAttribute.getAccountId())
                                          .withEntityId(settingAttribute.getUuid())
                                          .withType(Type.SETTING)
                                          .withYaml(beforeYaml)
                                          .build();
      yamlHistoryService.save(beforeYamLVersion);

      rr.setResource(settingAttribute);
    }

    return rr;
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

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  public RestResponse<Environment> updateEnvironment(String accountId, YamlPayload yamlPayload) {
    return yamlSyncService.update(yamlPayload, accountId);
  }

  public RestResponse<Service> updateService(String accountId, YamlPayload yamlPayload) {
    return yamlSyncService.update(yamlPayload, accountId);
  }
}
