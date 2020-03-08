package software.wings.beans.command;

import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.WingsException;
import org.mongodb.morphia.annotations.Transient;

import java.util.HashMap;
import java.util.Map;
public class InitPowerShellCommandUnit extends AbstractCommandUnit {
  @Inject @Transient private transient CommandUnitHelper commandUnitHelper;
  public static final transient String INIT_POWERSHELL_UNIT_NAME = "Initialize";

  @JsonIgnore @SchemaIgnore @Transient private Command command;

  @JsonIgnore @SchemaIgnore private String activityId;

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> envVariables = new HashMap<>();

  @JsonIgnore @Transient @SchemaIgnore private Map<String, String> safeDisplayEnvVariables = new HashMap<>();

  public InitPowerShellCommandUnit() {
    super(CommandUnitType.EXEC);
    setName(INIT_POWERSHELL_UNIT_NAME);
  }

  private String getInitCommand(String runtimePath) {
    String script = "$RUNTIME_PATH=[System.Environment]::ExpandEnvironmentVariables(\"%s\")%n"
        + "if(!(Test-Path \"$RUNTIME_PATH\"))%n"
        + "{%n"
        + "    New-Item -ItemType Directory -Path \"$RUNTIME_PATH\"%n"
        + "    Write-Host \"$RUNTIME_PATH Folder Created Successfully.\"%n"
        + "}%n"
        + "else%n"
        + "{%n"
        + "    Write-Host \"${RUNTIME_PATH} Folder already exists.\"%n"
        + "}";

    return String.format(script, runtimePath);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    notNullCheck("Service Variables", context.getServiceVariables());
    envVariables.putAll(context.getServiceVariables());

    commandUnitHelper.addArtifactFileNameToEnv(envVariables, context);
    notNullCheck("Safe Display Service Variables", context.getSafeDisplayServiceVariables());
    safeDisplayEnvVariables.putAll(context.getSafeDisplayServiceVariables());

    createPreparedCommands(command);
    context.addEnvVariables(envVariables);
    if (!(context instanceof ShellCommandExecutionContext)) {
      throw new WingsException("Unexpected context type");
    }
    return ((ShellCommandExecutionContext) context)
        .executeCommandString(getInitCommand(context.getWindowsRuntimePath()));
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
