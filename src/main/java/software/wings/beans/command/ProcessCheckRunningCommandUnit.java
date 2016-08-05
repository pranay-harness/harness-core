package software.wings.beans.command;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.template.TemplateException;
import software.wings.stencils.DefaultValue;

import java.io.IOException;
import java.util.List;

/**
 * Created by anubhaw on 7/14/16.
 */
public class ProcessCheckRunningCommandUnit extends ExecCommandUnit {
  @DefaultValue("tomcat")
  @Attributes(title = "Expression", description = "ex. catalina.base=/path/, tomcat ... ")
  private String expression = "abc";

  /**
   * Instantiates a new Process check command unit.
   */
  public ProcessCheckRunningCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PROCESS_CHECK_RUNNING);
  }

  @Override
  public List<String> prepare(String activityId, String executionStagingDir, String launcherScriptFileName,
      String prefix) throws IOException, TemplateException {
    setCommandString(String.format("pgrep -f '%s'", expression.trim()));
    return super.prepare(activityId, executionStagingDir, launcherScriptFileName, prefix);
  }

  @SchemaIgnore
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @SchemaIgnore
  @Override
  public String getCommandString() {
    return super.getCommandString();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }

  @SchemaIgnore
  @Override
  public boolean isTailFiles() {
    return super.isTailFiles();
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
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
