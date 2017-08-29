package software.wings.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class SetupYaml extends GenericYaml {
  @YamlSerialize public List<String> applications;

  @JsonIgnore
  public List<String> getAppNames() {
    return applications;
  }

  public void setAppNames(List<String> appNames) {
    this.applications = appNames;
  }
}
