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
@JsonTypeName("PROCESS_CHECK_RUNNING")
public class ProcessCheckRunningCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Process check command unit.
   */
  public ProcessCheckRunningCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.PROCESS_CHECK_RUNNING);
  }

  @SchemaIgnore
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @Attributes(title = "Command")
  @DefaultValue("set -x\npgrep -f \"\\-Dcatalina.home=$WINGS_RUNTIME_PATH/tomcat\"")
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
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ExecCommandUnit.Yaml {
    public static final class Builder extends ExecCommandUnit.Yaml.Builder {
      private Builder() {}

      public static Builder anYaml() {
        return new Builder();
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setWorkingDirectory(workingDirectory);
        yaml.setCommand(command);
        yaml.setFilePatternEntryList(filePatternEntryList);
        yaml.setName(name);
        yaml.setCommandUnitType(commandUnitType);
        yaml.setDeploymentType(deploymentType);
        return yaml;
      }

      @Override
      protected Yaml getCommandUnitYaml() {
        return new Yaml();
      }
    }
  }
}
