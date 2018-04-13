package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.stencils.DefaultValue;

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
    super();
    setCommandUnitType(CommandUnitType.PORT_CHECK_LISTENING);
  }

  @DefaultValue("set -x\nnc -v -z -w 5 localhost 8080")
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("PORT_CHECK_LISTENING")
  public static class Yaml extends ExecCommandUnit.AbstractYaml {
    public Yaml() {
      super(CommandUnitType.PORT_CHECK_LISTENING.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType, String workingDirectory, String scriptType, String command,
        List<TailFilePatternEntry.Yaml> filePatternEntryList) {
      super(name, CommandUnitType.PORT_CHECK_LISTENING.name(), deploymentType, workingDirectory, scriptType, command,
          filePatternEntryList);
    }
  }
}
