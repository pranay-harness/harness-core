package software.wings.service.impl;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.YamlResourceService;
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

  private final Logger logger = LoggerFactory.getLogger(getClass());

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

      if (serviceCommand.isTargetToAllEnv()) {
        if (environments != null) {
          for (Environment env : environments) {
            YamlTargetEnvironment targetEnv = new YamlTargetEnvironment();
            targetEnv.setName(targetEnv.getName());
            targetEnv.setVersion("default");
            serviceCommandYaml.getTargetEnvironments().add(targetEnv);
          }
        }
      } else {
        Map<String, EntityVersion> envMap = serviceCommand.getEnvIdVersionMap();

        logger.info("********* envMap: " + envMap);
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
                  ((YamlExecCommandUnit) ycu).setCommandPath(((ExecCommandUnit) cu).getCommandPath());
                  ((YamlExecCommandUnit) ycu).setCommandString(((ExecCommandUnit) cu).getCommandString());
                  break;
                case SCP:
                  ycu = new YamlScpCommandUnit();
                  ((YamlScpCommandUnit) ycu).setFileCategory(((ScpCommandUnit) cu).getFileCategory().getName());
                  ((YamlScpCommandUnit) ycu)
                      .setDestinationDirectoryPath(((ScpCommandUnit) cu).getDestinationDirectoryPath());
                  break;
                case COPY_CONFIGS:
                  ycu = new YamlCopyConfigCommandUnit();
                  ((YamlCopyConfigCommandUnit) ycu)
                      .setDestinationParentPath(((CopyConfigCommandUnit) cu).getDestinationParentPath());
                  break;
                case COMMAND:
                  ycu = new YamlCommandRefCommandUnit();
                  ((YamlCommandRefCommandUnit) ycu).setReferenceId(command.getReferenceId());
                  ((YamlCommandRefCommandUnit) ycu).setCommandType("OTHER");
                  break;
                case SETUP_ENV:
                  ycu = new YamlSetupEnvCommandUnit();
                  ((YamlSetupEnvCommandUnit) ycu).setCommandString(((SetupEnvCommandUnit) cu).getCommandString());
                  break;

                  // TODO - NEED DOCKER AND KUBERNETES TYPES

                default:
                  // handle unfound
                  ycu = new YamlCommandUnit();
              }

              ycu.setName(cu.getName());
              ycu.setCommandUnitType(cut.getName());

              ycv.getCommandUnits().add(ycu);
            }
          }

          serviceCommandYaml.getVersions().add(ycv);
        }
      }
    }

    return YamlHelper.getYamlRestResponse(serviceCommandYaml, "setup.yaml");
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
  public ServiceCommand updateServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String serviceCommandId, YamlPayload yamlPayload) {
    // TODO - needs implementation
    return null;
  }
}
