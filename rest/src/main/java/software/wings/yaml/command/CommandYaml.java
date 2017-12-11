package software.wings.yaml.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;

/**
 * ServiceCommand and Command are merged in yaml to provide a simplistic user configuration experience.
 * @author rktummala on 11/09/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandYaml extends BaseEntityYaml {
  private String name;
  private String commandUnitType;
  private boolean targetToAllEnv;
  private List<String> targetEnvs = new ArrayList<>();
  private List<AbstractCommandUnit.Yaml> commandUnits = new ArrayList<>();

  public static final class Builder {
    private String name;
    private String commandUnitType;
    private boolean targetToAllEnv;
    private List<String> targetEnvs = new ArrayList<>();
    private List<AbstractCommandUnit.Yaml> commandUnits = new ArrayList<>();
    private String type;

    private Builder() {}

    public static Builder aYaml() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withCommandUnitType(String commandUnitType) {
      this.commandUnitType = commandUnitType;
      return this;
    }

    public Builder withTargetToAllEnv(boolean targetToAllEnv) {
      this.targetToAllEnv = targetToAllEnv;
      return this;
    }

    public Builder withTargetEnvs(List<String> targetEnvs) {
      this.targetEnvs = targetEnvs;
      return this;
    }

    public Builder withCommandUnits(List<AbstractCommandUnit.Yaml> commandUnits) {
      this.commandUnits = commandUnits;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder but() {
      return aYaml()
          .withName(name)
          .withCommandUnitType(commandUnitType)
          .withTargetToAllEnv(targetToAllEnv)
          .withTargetEnvs(targetEnvs)
          .withCommandUnits(commandUnits)
          .withType(type);
    }

    public CommandYaml build() {
      CommandYaml serviceCommandYaml = new CommandYaml();
      serviceCommandYaml.setType(type);
      serviceCommandYaml.commandUnitType = this.commandUnitType;
      serviceCommandYaml.name = this.name;
      serviceCommandYaml.commandUnits = this.commandUnits;
      serviceCommandYaml.targetToAllEnv = this.targetToAllEnv;
      serviceCommandYaml.targetEnvs = this.targetEnvs;
      return serviceCommandYaml;
    }
  }
}
