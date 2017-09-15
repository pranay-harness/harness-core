package software.wings.yaml;

public class OrchestrationStreamActionYaml extends StreamActionYaml {
  @YamlSerialize public String envName;

  public OrchestrationStreamActionYaml() {
    super();
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }
}
