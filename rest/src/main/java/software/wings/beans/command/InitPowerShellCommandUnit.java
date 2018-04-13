package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.utils.Util.escapifyString;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.common.Constants;
import software.wings.utils.Validator;

import java.util.Map;
public class InitPowerShellCommandUnit extends AbstractCommandUnit {
  public static final transient String INIT_POWERSHELL_UNIT_NAME = "Initialize";

  @JsonIgnore @SchemaIgnore @Transient private Command command;

  @JsonIgnore @SchemaIgnore private String activityId;

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> envVariables = Maps.newHashMap();

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> safeDisplayEnvVariables = Maps.newHashMap();

  @JsonIgnore
  @Transient
  @SchemaIgnore
  protected static final Logger logger = LoggerFactory.getLogger(InitPowerShellCommandUnit.class);

  public InitPowerShellCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(INIT_POWERSHELL_UNIT_NAME);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    activityId = context.getActivityId();

    Validator.notNullCheck("Service Variables", context.getServiceVariables());
    for (Map.Entry<String, String> entry : context.getServiceVariables().entrySet()) {
      envVariables.put(entry.getKey(), escapifyString(entry.getValue()));
    }
    envVariables.put("WINGS_STAGING_PATH", context.getStagingPath());
    envVariables.put("WINGS_RUNTIME_PATH", context.getRuntimePath());
    envVariables.put("WINGS_BACKUP_PATH", context.getBackupPath());
    if (isNotEmpty(context.getArtifactFiles())) {
      String name = context.getArtifactFiles().get(0).getName();
      if (isNotEmpty(name)) {
        envVariables.put("ARTIFACT_FILE_NAME", name);
      }
    } else if (context.getMetadata() != null) {
      String value = context.getMetadata().get(Constants.ARTIFACT_FILE_NAME);
      if (isNotEmpty(value)) {
        envVariables.put("ARTIFACT_FILE_NAME", value);
      }
    }

    Validator.notNullCheck("Safe Display Service Variables", context.getSafeDisplayServiceVariables());
    for (Map.Entry<String, String> entry : context.getSafeDisplayServiceVariables().entrySet()) {
      safeDisplayEnvVariables.put(entry.getKey(), escapifyString(entry.getValue()));
    }

    createPreparedCommands(command);

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.SUCCESS;

    context.addEnvVariables(envVariables);
    return commandExecutionStatus;
  }

  private void createPreparedCommands(Command command) {
    for (CommandUnit unit : command.getCommandUnits()) {
      if (unit instanceof Command) {
        createPreparedCommands((Command) unit);
      } else {
        if (unit instanceof ExecCommandUnit) {
          ((ExecCommandUnit) unit).setPreparedCommand(((ExecCommandUnit) unit).getCommandString());
        }
      }
    }
  }

  public void setCommand(Command command) {
    this.command = command;
  }
}
