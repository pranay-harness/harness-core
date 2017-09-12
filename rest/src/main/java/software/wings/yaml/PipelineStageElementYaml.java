package software.wings.yaml;

public class PipelineStageElementYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String type;
  @YamlSerialize public String envName;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }
}
