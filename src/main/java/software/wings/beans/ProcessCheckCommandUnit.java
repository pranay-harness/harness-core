package software.wings.beans;

import com.google.common.base.Strings;

import com.github.reinert.jjschema.Attributes;

/**
 * Created by anubhaw on 7/14/16.
 */
public class ProcessCheckCommandUnit extends ExecCommandUnit {
  @Attributes(title = "Expression", description = "ex. catalina.base=/path/, tomcat ... ")
  private String expression = "abc";

  /**
   * Instantiates a new Process check command unit.
   */
  public ProcessCheckCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PROCESS_CHECK);
  }

  @Override
  public void setup(CommandExecutionContext context) {
    setCommandString(String.format("pgrep -f '%s'", expression.trim()));
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
  public String getExpression() {
    return expression;
  }

  /**
   * Sets process name.
   *
   * @param expression the process name
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }
}
