package software.wings.service.impl.yaml;

import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
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
import software.wings.yaml.command.ServiceCommandYaml;
import software.wings.yaml.command.YamlCommandRefCommandUnit;
import software.wings.yaml.command.YamlCommandUnit;
import software.wings.yaml.command.YamlCopyConfigCommandUnit;
import software.wings.yaml.command.YamlExecCommandUnit;
import software.wings.yaml.command.YamlScpCommandUnit;
import software.wings.yaml.command.YamlSetupEnvCommandUnit;
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
                  YamlCommandUnit ycu;

                  CommandUnitType cut = cu.getCommandUnitType();

                  switch (cut) {
                    case EXEC:
                      ycu = new YamlExecCommandUnit();
                      ycu.setName(cu.getName());
                      ycu.setCommandUnitType(cut.getName());
                      ((YamlExecCommandUnit) ycu).setCommandPath(((ExecCommandUnit) cu).getCommandPath());
                      ((YamlExecCommandUnit) ycu).setCommandString(((ExecCommandUnit) cu).getCommandString());
                      break;
                    case SCP:
                      ycu = new YamlScpCommandUnit();
                      ycu.setName(cu.getName());
                      ycu.setCommandUnitType(cut.getName());
                      ((YamlScpCommandUnit) ycu).setFileCategory(((ScpCommandUnit) cu).getFileCategory().getName());
                      ((YamlScpCommandUnit) ycu)
                          .setDestinationDirectoryPath(((ScpCommandUnit) cu).getDestinationDirectoryPath());
                      break;
                    case COPY_CONFIGS:
                      ycu = new YamlCopyConfigCommandUnit();
                      ycu.setName(cu.getName());
                      ycu.setCommandUnitType(cut.getName());
                      ((YamlCopyConfigCommandUnit) ycu)
                          .setDestinationParentPath(((CopyConfigCommandUnit) cu).getDestinationParentPath());
                      break;
                    case COMMAND:
                      ycu = new YamlCommandRefCommandUnit();
                      ycu.setName(cu.getName());
                      ycu.setCommandUnitType(cut.getName());
                      ((YamlCommandRefCommandUnit) ycu).setReferenceId(((Command) cu).getReferenceId());
                      ((YamlCommandRefCommandUnit) ycu).setCommandType(((Command) cu).getCommandType().name());
                      break;
                    case SETUP_ENV:
                      ycu = new YamlSetupEnvCommandUnit();
                      ycu.setName(cu.getName());
                      ycu.setCommandUnitType(cut.getName());
                      ((YamlSetupEnvCommandUnit) ycu).setCommandString(((SetupEnvCommandUnit) cu).getCommandString());
                      break;

                      // TODO - NEED DOCKER AND KUBERNETES TYPES

                    default:
                      // handle unfound
                      ycu = new YamlCommandUnit();
                  }

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

    return YamlHelper.getYamlRestResponse(serviceCommandYaml, serviceCommand.getName() + ".yaml");
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
    // TODO - needs implementation
    return null;
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

    return YamlHelper.getYamlRestResponse(pipelineYaml, pipeline.getName() + ".yaml");
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

    return YamlHelper.getYamlRestResponse(artifactStreamYaml, payLoadName + ".yaml");
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

    return YamlHelper.getYamlRestResponse(workflowYaml, workflow.getName() + ".yaml");
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
      return YamlHelper.getYamlRestResponse(settingAttributeYaml, settingAttribute.getName() + ".yaml");
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
    Environment environment = environmentService.get(appId, envId, true);
    EnvironmentYaml environmentYaml = new EnvironmentYaml(environment);

    return YamlHelper.getYamlRestResponse(environmentYaml, environment.getName() + ".yaml");
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
                                              .withEntityId(accountId)
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
