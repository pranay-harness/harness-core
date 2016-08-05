package software.wings.beans.command;

import static com.google.common.collect.ImmutableMap.of;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 8/3/16.
 */
public class PortCheckClearedCommandUnit extends ExecCommandUnit {
  public PortCheckClearedCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PORT_CHECK_CLEARED);
  }

  @DefaultValue("set -x\nnc -v -z -w 5 localhost 8080\nrc=$?"
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
  public boolean isTailFiles() {
    return super.isTailFiles();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
  }

  public static class PortCheckDataProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return of("true", "Free", "false", "Used");
    }
  }
}
