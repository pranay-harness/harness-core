package software.wings.yaml.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.BaseYaml;

@Data
@EqualsAndHashCode(callSuper = true)
public class TargetEnvYaml extends BaseYaml {
  public String envName;
  public String version;

  public static final class Builder {
    public String envName;
    public String version;

    private Builder() {}

    public static Builder aYaml() {
      return new Builder();
    }

    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder but() {
      return aYaml().withEnvName(envName).withVersion(version);
    }

    public TargetEnvYaml build() {
      TargetEnvYaml targetEnvYaml = new TargetEnvYaml();
      targetEnvYaml.setEnvName(envName);
      targetEnvYaml.setVersion(version);
      return targetEnvYaml;
    }
  }
}
