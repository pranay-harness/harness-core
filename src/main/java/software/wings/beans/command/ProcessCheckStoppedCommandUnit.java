package software.wings.beans.command;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 7/14/16.
 */
public class ProcessCheckStoppedCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Process check command unit.
   */
  public ProcessCheckStoppedCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PROCESS_CHECK_STOPPED);
  }

  @SchemaIgnore
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @Attributes(title = "Command")
  @DefaultValue("set -x\npgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"\nrc=$?"
      + "\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi")
  @Override
  public String
  getCommandString() {
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
}
