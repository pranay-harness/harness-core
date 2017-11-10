package software.wings.yaml.artifactstream;

public class OrchestrationStreamActionYaml extends StreamActionYaml {
  public String envName;

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
