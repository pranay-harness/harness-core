package software.wings.beans.command;

import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
@JsonTypeName("PORT_CHECK_LISTENING")
public class PortCheckListeningCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Port check listening command unit.
   */
  public PortCheckListeningCommandUnit() {
    setCommandUnitType(CommandUnitType.PORT_CHECK_LISTENING);
  }

  @DefaultValue("nc -v -z -w 5 localhost 8080")
  @Override
  public String getCommandString() {
    return super.getCommandString();
  }

  @SchemaIgnore
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }
}
