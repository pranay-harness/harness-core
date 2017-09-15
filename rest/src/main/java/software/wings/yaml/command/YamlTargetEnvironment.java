package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlTargetEnvironment {
  @YamlSerialize public String name;
  @YamlSerialize public String version;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
