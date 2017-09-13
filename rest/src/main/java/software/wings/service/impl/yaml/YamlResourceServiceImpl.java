package software.wings.service.impl.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
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
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.ArtifactStreamYaml;
import software.wings.yaml.OrchestrationStreamActionYaml;
import software.wings.yaml.PipelineStageElementYaml;
import software.wings.yaml.PipelineStageYaml;
import software.wings.yaml.PipelineYaml;
import software.wings.yaml.StreamActionYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.command.ServiceCommandYaml;
import software.wings.yaml.command.YamlCommandRefCommandUnit;
import software.wings.yaml.command.YamlCommandUnit;
import software.wings.yaml.command.YamlCommandVersion;
import software.wings.yaml.command.YamlCopyConfigCommandUnit;
import software.wings.yaml.command.YamlExecCommandUnit;
import software.wings.yaml.command.YamlScpCommandUnit;
import software.wings.yaml.command.YamlSetupEnvCommandUnit;
import software.wings.yaml.command.YamlTargetEnvironment;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class YamlResourceServiceImpl implements YamlResourceService {
  @Inject private CommandService commandService;
  @Inject private EnvironmentService environmentService;
  @Inject private PipelineService pipelineService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ServiceResourceService serviceResourceService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String ENV_ID_PROPERTY = "envId";

  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @return the application
   */
  public RestResponse<YamlPayload> getServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String serviceCommandId) {
    ServiceCommandYaml serviceCommandYaml = new ServiceCommandYaml();

    List<Environment> environments = environmentService.getEnvByApp(appId);

    ServiceCommand serviceCommand = commandService.getServiceCommand(appId, serviceCommandId);

    if (serviceCommand != null) {
      serviceCommandYaml.setName(serviceCommand.getName());
      serviceCommandYaml.setDefaultVersion(serviceCommand.getDefaultVersion());

      if (environments != null) {
        Map<String, EntityVersion> envMap = serviceCommand.getEnvIdVersionMap();

        for (Environment env : environments) {
          YamlTargetEnvironment targetEnv = new YamlTargetEnvironment();
          targetEnv.setName(env.getName());

          String envId = env.getUuid();

          if (envId != null && envMap.containsKey(envId)) {
            EntityVersion et = envMap.get(envId);
            targetEnv.setVersion(new Integer(et.getVersion()).toString());
          } else {
            targetEnv.setVersion("default");
          }

          serviceCommandYaml.getTargetEnvironments().add(targetEnv);
        }
      } else {
        // handle missing environments (should never happen)
        RestResponse rr = new RestResponse<>();
        YamlHelper.addResponseMessage(
            rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR, "The Environments are NULL!");
        return rr;
      }

      List<Command> commands = commandService.getCommandList(appId, serviceCommandId);

      if (commands != null && commands.size() > 0) {
        serviceCommandYaml.setCommandUnitType(commands.get(0).getCommandUnitType().getName());
        serviceCommandYaml.setCommandType(commands.get(0).getCommandType().toString());

        for (Command command : commands) {
          YamlCommandVersion ycv = new YamlCommandVersion();
          ycv.setVersion(command.getVersion());

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

              ycv.getCommandUnits().add(ycu);
            }
          }

          serviceCommandYaml.getVersions().add(ycv);
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
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @param yamlPayload the yaml version of the service command
   * @return the application
   */
  public ServiceCommand updateServiceCommand(@NotEmpty String appId, @NotEmpty String serviceId,
      @NotEmpty String serviceCommandId, YamlPayload yamlPayload, boolean deleteEnabled) {
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
  public Pipeline updatePipeline(String appId, String pipelineId, YamlPayload yamlPayload, boolean deleteEnabled) {
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
  public ArtifactStream updateTrigger(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled) {
    return null;
  }
}
