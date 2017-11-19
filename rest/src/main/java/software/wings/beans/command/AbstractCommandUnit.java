package software.wings.beans.command;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.yaml.BaseYaml;

/**
 * Created by anubhaw on 5/25/16.
 */
@Data
public abstract class AbstractCommandUnit implements CommandUnit {
  @SchemaIgnore private String name;
  private CommandUnitType commandUnitType;
  @SchemaIgnore private CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.QUEUED;
  @SchemaIgnore private boolean artifactNeeded = false;
  @SchemaIgnore private String deploymentType;

  /**
   * Instantiates a new Command unit.
   */
  public AbstractCommandUnit() {}

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public AbstractCommandUnit(CommandUnitType commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static abstract class Yaml extends BaseYaml {
    private String name;
    private String commandUnitType;
    private String deploymentType;

    public static abstract class Builder {
      protected String name;
      protected String commandUnitType;
      protected String deploymentType;

      protected Builder() {}

      public Builder withName(String name) {
        this.name = name;
        return this;
      }

      public Builder withCommandUnitType(String commandUnitType) {
        this.commandUnitType = commandUnitType;
        return this;
      }

      public Builder withDeploymentType(String deploymentType) {
        this.deploymentType = deploymentType;
        return this;
      }

      public <T extends AbstractCommandUnit.Yaml> T build() {
        T yaml = getCommandUnitYaml();
        yaml.setName(name);
        yaml.setCommandUnitType(commandUnitType);
        yaml.setDeploymentType(deploymentType);
        return yaml;
      }

      protected abstract <T extends AbstractCommandUnit.Yaml> T getCommandUnitYaml();
    }
  }
}
