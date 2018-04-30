package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 7/14/16.
 */
@JsonTypeName("PROCESS_CHECK_STOPPED")
public class ProcessCheckStoppedCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Process check command unit.
   */
  public ProcessCheckStoppedCommandUnit() {
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
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("PROCESS_CHECK_STOPPED")
  public static class Yaml extends ExecCommandUnit.AbstractYaml {
    public Yaml() {
      super(CommandUnitType.PROCESS_CHECK_STOPPED.name());
    }

    @lombok.Builder
    public Yaml(String name, String deploymentType, String workingDirectory, String scriptType, String command,
        List<TailFilePatternEntry.Yaml> filePatternEntryList) {
      super(name, CommandUnitType.PROCESS_CHECK_STOPPED.name(), deploymentType, workingDirectory, scriptType, command,
          filePatternEntryList);
    }
  }
}
