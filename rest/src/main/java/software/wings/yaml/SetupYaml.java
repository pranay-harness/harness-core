package software.wings.yaml;

import java.util.List;

public class SetupYaml {
  @YamlSerialize public List<String> applications;

  public List<String> getApplications() {
    return applications;
  }

  public void setApplications(List<String> applications) {
    this.applications = applications;
  }
}
