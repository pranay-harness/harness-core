package software.wings.service.impl.yaml;

import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.beans.SplunkConfig;
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
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.ArtifactStreamYaml;
import software.wings.yaml.EnvironmentYaml;
import software.wings.yaml.OrchestrationStreamActionYaml;
import software.wings.yaml.PipelineStageElementYaml;
import software.wings.yaml.PipelineStageYaml;
import software.wings.yaml.PipelineYaml;
import software.wings.yaml.StreamActionYaml;
import software.wings.yaml.WorkflowYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;
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
import software.wings.yaml.settingAttribute.JenkinsYaml;
import software.wings.yaml.settingAttribute.LogzYaml;
import software.wings.yaml.settingAttribute.NexusYaml;
import software.wings.yaml.settingAttribute.PhysicalDataCenterYaml;
import software.wings.yaml.settingAttribute.SettingAttributeYaml;
import software.wings.yaml.settingAttribute.SlackYaml;
import software.wings.yaml.settingAttribute.SmtpYaml;
import software.wings.yaml.settingAttribute.SplunkYaml;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class YamlResourceServiceImpl implements YamlResourceService {
  @Inject private AppService appService;
  @Inject private YamlHistoryService yamlHistoryService;
  @Inject private CommandService commandService;
  @Inject private EnvironmentService environmentService;
  @Inject private PipelineService pipelineService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowService workflowService;
  @Inject private SettingsService settingsService;
  @Inject private YamlGitSyncService yamlGitSyncService;

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

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, serviceCommand.getUuid(), serviceCommandYaml, serviceCommand.getName() + ".yaml");
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
    PipelineYaml pipelineYaml = new PipelineYaml();

    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);

    pipelineYaml.setName(pipeline.getName());
    pipelineYaml.setDescription(pipeline.getDescription());

    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();

    if (pipelineStages != null) {
      for (PipelineStage ps : pipelineStages) {
        PipelineStageYaml pipelineStageYaml = new PipelineStageYaml();

        List<PipelineStageElement> stageElements = ps.getPipelineStageElements();

        if (stageElements != null) {
          for (PipelineStageElement se : stageElements) {
            PipelineStageElementYaml pipelineStageElementYaml = new PipelineStageElementYaml();

            pipelineStageElementYaml.setName(se.getName());
            pipelineStageElementYaml.setType(se.getType());

            Map<String, Object> theMap = se.getProperties();

            if (theMap.containsKey(ENV_ID_PROPERTY)) {
              String envId = (String) theMap.get(ENV_ID_PROPERTY);
              pipelineStageElementYaml.setEnvName(environmentService.get(appId, envId, false).getName());
            }

            pipelineStageYaml.getPipelineStageElements().add(pipelineStageElementYaml);
          }
        }
        pipelineYaml.getPipelineStages().add(pipelineStageYaml);
      }
    }

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, pipeline.getUuid(), pipelineYaml, pipeline.getName() + ".yaml");
  }

  /**
   * Update a pipeline that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId     the app id
   * @param pipelineId the pipeline id
   * @param yamlPayload the yaml version of the service command
   * @return the rest response
   */
  public RestResponse<Pipeline> updatePipeline(
      String appId, String pipelineId, YamlPayload yamlPayload, boolean deleteEnabled) {
    // TODO - needs implementation
    return null;
  }

  /**
   * Gets the yaml version of a trigger by artifactStreamId
   *
   * @param appId     the app id
   * @param artifactStreamId the artifact stream id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getTrigger(String appId, String artifactStreamId) {
    ArtifactStreamYaml artifactStreamYaml = new ArtifactStreamYaml();

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);

    if (artifactStream == null) {
      // handle missing artifactStream
      RestResponse rr = new RestResponse<>();
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
          "The ArtifactStream with appId: '" + appId + "' and artifactStreamId: '" + artifactStreamId
              + "' was not found!");
      return rr;
    }

    artifactStreamYaml.setArtifactStreamType(artifactStream.getArtifactStreamType());
    artifactStreamYaml.setSourceName(artifactStream.getSourceName());

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

    artifactStreamYaml.setServiceName(serviceName);

    List<ArtifactStreamAction> streamActions = artifactStream.getStreamActions();

    if (streamActions != null) {
      for (ArtifactStreamAction sa : streamActions) {
        StreamActionYaml say;

        switch (sa.getWorkflowType()) {
          case ORCHESTRATION:
            say = new OrchestrationStreamActionYaml();
            say.setWorkflowName(sa.getWorkflowName());
            say.setWorkflowType(sa.getWorkflowType().name());
            ((OrchestrationStreamActionYaml) say).setEnvName(sa.getEnvName());
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

        artifactStreamYaml.getStreamActions().add(say);
      }
    }

    String payLoadName = artifactStream.getSourceName() + "(" + serviceName + ")";

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, artifactStream.getUuid(), artifactStreamYaml, payLoadName + ".yaml");
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
    WorkflowYaml workflowYaml = new WorkflowYaml();

    Workflow workflow = workflowService.readWorkflow(appId, workflowId);

    workflowYaml.setName(workflow.getName());
    workflowYaml.setDescription(workflow.getDescription());
    workflowYaml.setWorkflowType(workflow.getWorkflowType().name());

    // TODO - this is the general format (as per Rishi) that we need to construct - WorkflowYaml will need to be
    // extended
    /*
    Workflow
      name
      type
      …
      orchestrationWorkflow
        type: CANARY
        preDeploymentSteps
          [
            {
              name:ChangeManagementStart
              type: HTTP
              properties{
                url:
                method: GET
              }
            }
          ]

        phases[
          {
            Disable Services:
              steps:
                [
                  {
                    name:ChangeManagementStart
                    type: HTTP
                    properties{
                      url:
                      method: GET
                    }
                  }
                  ... more steps
                ],
              customFailureStrategies{
              }
              isParallel:true
              waitInterval:34
            },
            ... more phases
    */

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, workflow.getUuid(), workflowYaml, workflow.getName() + ".yaml");
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

    SettingAttributeYaml settingAttributeYaml = null;

    switch (settingVariableType) {
      // cloud providers
      case AWS:
        settingAttributeYaml = new AwsYaml(settingAttribute);
        break;
      case GCP:
        settingAttributeYaml = new GcpYaml(settingAttribute);
        break;
      case PHYSICAL_DATA_CENTER:
        settingAttributeYaml = new PhysicalDataCenterYaml(settingAttribute);
        break;

      // artifact servers
      case JENKINS:
        settingAttributeYaml = new JenkinsYaml(settingAttribute);
        break;
      case BAMBOO:
        settingAttributeYaml = new BambooYaml(settingAttribute);
        break;
      case DOCKER:
        settingAttributeYaml = new DockerYaml(settingAttribute);
        break;
      case NEXUS:
        settingAttributeYaml = new NexusYaml(settingAttribute);
        break;
      case ARTIFACTORY:
        settingAttributeYaml = new ArtifactoryYaml(settingAttribute);
        break;

      // collaboration providers
      case SMTP:
        settingAttributeYaml = new SmtpYaml(settingAttribute);
        break;
      case SLACK:
        settingAttributeYaml = new SlackYaml(settingAttribute);
        break;

      // load balancers
      case ELB:
        settingAttributeYaml = new ElbYaml(settingAttribute);
        break;

      // verification providers
      // JENKINS is also a (logical) part of this group
      case APP_DYNAMICS:
        settingAttributeYaml = new AppDynamicsYaml(settingAttribute);
        break;
      case SPLUNK:
        settingAttributeYaml = new SplunkYaml(settingAttribute);
        break;
      case ELK:
        settingAttributeYaml = new ElkYaml(settingAttribute);
        break;
      case LOGZ:
        settingAttributeYaml = new LogzYaml(settingAttribute);
        break;
      default:
        // handle not found
        RestResponse rr = new RestResponse<>();
        YamlHelper.addUnknownSettingVariableTypeMessage(rr, settingVariableType);
        return rr;
    }

    if (settingAttributeYaml != null) {
      return YamlHelper.getYamlRestResponse(
          yamlGitSyncService, settingAttribute.getUuid(), settingAttributeYaml, settingAttribute.getName() + ".yaml");
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
    SettingAttributeYaml beforeSettingAttributeYaml = null;

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

            beforeConfig = (AwsConfig) settingAttribute.getValue();
            settingAttribute.setName(awsYaml.getName());
            config = AwsConfig.Builder.anAwsConfig()
                         .withAccountId(accountId)
                         .withAccessKey(awsYaml.getAccessKey())
                         .withSecretKey(((AwsConfig) beforeConfig).getSecretKey())
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
            config = GcpConfig.GcpConfigBuilder.aGcpConfig()
                         .withServiceAccountKeyFileContent(gcpYaml.getServiceAccountKeyFileContent())
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

            beforeConfig = (JenkinsConfig) settingAttribute.getValue();
            settingAttribute.setName(jenkinsYaml.getName());
            config = JenkinsConfig.Builder.aJenkinsConfig()
                         .withAccountId(accountId)
                         .withJenkinsUrl(jenkinsYaml.getUrl())
                         .withPassword(((JenkinsConfig) beforeConfig).getPassword())
                         .withUsername(jenkinsYaml.getUsername())
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

            beforeConfig = (BambooConfig) settingAttribute.getValue();
            settingAttribute.setName(bambooYaml.getName());
            config = BambooConfig.Builder.aBambooConfig()
                         .withAccountId(accountId)
                         .withBambooUrl(bambooYaml.getUrl())
                         .withPassword(((BambooConfig) beforeConfig).getPassword())
                         .withUsername(bambooYaml.getUsername())
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

            beforeConfig = (DockerConfig) settingAttribute.getValue();
            settingAttribute.setName(dockerYaml.getName());
            config = DockerConfig.Builder.aDockerConfig()
                         .withAccountId(accountId)
                         .withDockerRegistryUrl(dockerYaml.getUrl())
                         .withPassword(((DockerConfig) beforeConfig).getPassword())
                         .withUsername(dockerYaml.getUsername())
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

            beforeConfig = (NexusConfig) settingAttribute.getValue();
            settingAttribute.setName(nexusYaml.getName());
            config = NexusConfig.Builder.aNexusConfig()
                         .withAccountId(accountId)
                         .withNexusUrl(nexusYaml.getUrl())
                         .withPassword(((NexusConfig) beforeConfig).getPassword())
                         .withUsername(nexusYaml.getUsername())
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

            beforeConfig = (ArtifactoryConfig) settingAttribute.getValue();
            settingAttribute.setName(artifactoryYaml.getName());
            config = ArtifactoryConfig.Builder.anArtifactoryConfig()
                         .withAccountId(accountId)
                         .withArtifactoryUrl(artifactoryYaml.getUrl())
                         .withPassword(((ArtifactoryConfig) beforeConfig).getPassword())
                         .withUsername(artifactoryYaml.getUsername())
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

            beforeConfig = (SmtpConfig) settingAttribute.getValue();
            settingAttribute.setName(smtpYaml.getName());
            config = SmtpConfig.Builder.aSmtpConfig()
                         .withAccountId(accountId)
                         .withFromAddress(smtpYaml.getFromAddress())
                         .withHost(smtpYaml.getHost())
                         .withPassword(((SmtpConfig) beforeConfig).getPassword())
                         .withPort(smtpYaml.getPort())
                         .withUsername(smtpYaml.getUsername())
                         .withUseSSL(smtpYaml.isUseSSL())
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

            beforeConfig = (SlackConfig) settingAttribute.getValue();
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

            beforeConfig = (ElasticLoadBalancerConfig) settingAttribute.getValue();
            settingAttribute.setName(elbYaml.getName());
            config = ElasticLoadBalancerConfig.Builder.anElasticLoadBalancerConfig()
                         .withAccountId(accountId)
                         .withAccessKey(elbYaml.getAccessKey())
                         .withLoadBalancerName(elbYaml.getLoadBalancerName())
                         .withSecretKey(((ElasticLoadBalancerConfig) beforeConfig).getSecretKey())
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

            beforeConfig = (AppDynamicsConfig) settingAttribute.getValue();
            settingAttribute.setName(appDynamicsYaml.getName());
            config = AppDynamicsConfig.Builder.anAppDynamicsConfig()
                         .withAccountId(accountId)
                         .withAccountname(appDynamicsYaml.getAccountname())
                         .withControllerUrl(appDynamicsYaml.getUrl())
                         .withPassword(((AppDynamicsConfig) beforeConfig).getPassword())
                         .withUsername(appDynamicsYaml.getUsername())
                         .build();
            settingAttribute.setValue(config);
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

            beforeConfig = (SplunkConfig) settingAttribute.getValue();
            settingAttribute.setName(splunkYaml.getName());
            config = SplunkConfig.builder()
                         .accountId(accountId)
                         .password(((SplunkConfig) beforeConfig).getPassword())
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

            beforeConfig = (ElkConfig) settingAttribute.getValue();
            settingAttribute.setName(elkYaml.getName());
            config = new ElkConfig();
            ((ElkConfig) config).setAccountId(accountId);
            ((ElkConfig) config).setUrl(elkYaml.getUrl());
            ((ElkConfig) config).setPassword(((ElkConfig) beforeConfig).getPassword());
            ((ElkConfig) config).setUsername(elkYaml.getUsername());
            settingAttribute.setValue(config);
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

            beforeConfig = (LogzConfig) settingAttribute.getValue();
            settingAttribute.setName(logzYaml.getName());
            config = new LogzConfig();
            ((LogzConfig) config).setAccountId(accountId);
            ((LogzConfig) config).setLogzUrl(logzYaml.getUrl());
            ((LogzConfig) config).setToken(((LogzConfig) beforeConfig).getToken());
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
    Environment environment = environmentService.get(appId, envId, true);
    EnvironmentYaml environmentYaml = new EnvironmentYaml(environment);

    return YamlHelper.getYamlRestResponse(
        yamlGitSyncService, environment.getUuid(), environmentYaml, environment.getName() + ".yaml");
  }

  /**
   * Update a environment that is sent as Yaml (in a JSON "wrapper")
   *
   * @param envId  the environment id
   * @param yamlPayload the yaml version of environment
   * @return the rest response
   */
  public RestResponse<Environment> updateEnvironment(
      String appId, String envId, YamlPayload yamlPayload, boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // get the before Yaml
    RestResponse beforeResponse = getEnvironment(appId, envId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (yaml.equals(beforeYaml)) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    EnvironmentYaml beforeEnvironmentYaml = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      try {
        beforeEnvironmentYaml = mapper.readValue(beforeYaml, EnvironmentYaml.class);
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

    EnvironmentYaml environmentYaml = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        Environment environment = environmentService.get(appId, envId, false);

        environmentYaml = mapper.readValue(yaml, EnvironmentYaml.class);

        // save the changes
        environment.setName(environmentYaml.getName());
        environment.setDescription(environmentYaml.getDescription());
        String environmentTypeStr = environmentYaml.getEnvironmentType().toUpperCase();

        try {
          EnvironmentType et = EnvironmentType.valueOf(environmentTypeStr);
          environment.setEnvironmentType(et);
        } catch (Exception e) {
          e.printStackTrace();
          YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
              "The EnvironmentType: '" + environmentTypeStr + "' is not found in the EnvironmentType Enum!");
          return rr;
        }

        environment = environmentService.update(environment);

        // return the new resource
        if (environment != null) {
          // save the before yaml version
          String accountId = appService.get(appId).getAccountId();
          YamlVersion beforeYamLVersion = aYamlVersion()
                                              .withAccountId(accountId)
                                              .withEntityId(environment.getUuid())
                                              .withType(Type.ENVIRONMENT)
                                              .withYaml(beforeYaml)
                                              .build();
          yamlHistoryService.save(beforeYamLVersion);

          rr.setResource(environment);
        }

      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        e.printStackTrace();
        YamlHelper.addUnrecognizedFieldsMessage(rr);
      }
    } else {
      // missing Yaml
      YamlHelper.addMissingYamlMessage(rr);
    }

    return rr;
  }
}
