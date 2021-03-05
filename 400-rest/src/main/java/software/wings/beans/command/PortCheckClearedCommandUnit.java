package software.wings.beans.command;

import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
@JsonTypeName("PORT_CHECK_CLEARED")
public class PortCheckClearedCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Port check cleared command unit.
   */
  public PortCheckClearedCommandUnit() {
    setCommandUnitType(CommandUnitType.PORT_CHECK_CLEARED);
  }

  @Attributes(title = "Command")
  @DefaultValue("nc -v -z -w 5 localhost 8080\nrc=$?"
      + "\nif [ \"$rc\" -eq 0 ]\nthen\nexit 1\nfi")
  @Override
  public String
  getCommandString() {
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
