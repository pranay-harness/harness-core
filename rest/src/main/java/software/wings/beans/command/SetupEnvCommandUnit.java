package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 7/12/16.
 */
@JsonTypeName("SETUP_ENV")
public class SetupEnvCommandUnit extends ExecCommandUnit {
  public static final String setupEnvCommandString = "\n"
      + "# Execute as root and pass environment variables\n"
      + "# su -p -\n\n"
      + "# Execute as root via user credentials (with root privileges)\n"
      + "# sudo -E su -p -\n\n"
      + "# The following variables are absolute paths defined as:\n"
      + "# ${HOME}/${appName}/${serviceName}/${serviceTemplateName}/[runtime|backup|staging]\n\n"
      + "mkdir -p \"$WINGS_RUNTIME_PATH\"\n"
      + "mkdir -p \"$WINGS_BACKUP_PATH\"\n"
      + "mkdir -p \"$WINGS_STAGING_PATH\"";

  /**
   * Instantiates a new Setup env command unit.
   */
  public SetupEnvCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.SETUP_ENV);
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @Attributes(title = "Command")
  @DefaultValue(setupEnvCommandString)
  @Override
  public String getCommandString() {
    return super.getCommandString();
  }

  @Attributes(title = "Working Directory")
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }

  @SchemaIgnore
  @Override
  public List<TailFilePatternEntry> getTailPatterns() {
    return super.getTailPatterns();
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
