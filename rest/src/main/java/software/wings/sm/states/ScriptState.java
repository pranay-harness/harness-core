package software.wings.sm.states;

import static software.wings.beans.FeatureName.SHELL_SCRIPT_AS_A_STEP;

import com.github.reinert.jjschema.Attributes;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import javax.inject.Inject;

public class ScriptState extends State {
  private static final Logger logger = LoggerFactory.getLogger(ScriptState.class);

  /**
   * Create a new Script State with given name.
   *
   * @param name name of the state.
   */
  public ScriptState(String name) {
    super(name, StateType.SCRIPT.name());
  }

  @Attributes(title = "Working Directory") @NotEmpty private String commandPath;

  @Attributes(title = "Script") @NotEmpty private String scriptString;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionResponse response = new ExecutionResponse();
    return response;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getCommandPath() {
    return commandPath;
  }

  public void setCommandPath(String commandPath) {
    this.commandPath = commandPath;
  }

  public String getScriptString() {
    return scriptString;
  }

  public void setScriptString(String scriptString) {
    this.scriptString = scriptString;
  }
}
