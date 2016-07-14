package software.wings.beans;

import com.google.common.base.Strings;

import com.github.reinert.jjschema.Attributes;

/**
 * Created by anubhaw on 7/14/16.
 */
public class ProcessCheckCommandUnit extends AbstractExecCommandUnit {
  @Attributes(title = "Process name", description = "ex. tomcat, java ... ") private String processName;

  /**
   * Instantiates a new Process check command unit.
   */
  public ProcessCheckCommandUnit() {
    super(CommandUnitType.PROCESS_CHECK);
    setProcessCommandOutput(true);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    setCommand("pgrep -f " + processName.trim());
  }

  @Override
  public CommandUnitExecutionResult processCommandOutput(String output) {
    CommandUnitExecutionResult stop = CommandUnitExecutionResult.STOP;
    stop.setExecutionResult(Strings.isNullOrEmpty(output) ? ExecutionResult.FAILURE : ExecutionResult.SUCCESS);
    return stop;
  }

  /**
   * Gets process name.
   *
   * @return the process name
   */
  public String getProcessName() {
    return processName;
  }

  /**
   * Sets process name.
   *
   * @param processName the process name
   */
  public void setProcessName(String processName) {
    this.processName = processName;
  }
}
