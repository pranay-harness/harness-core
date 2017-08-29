package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class OrderIndentTestYaml extends GenericYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public List<AppYaml> applications = new ArrayList<AppYaml>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<AppYaml> getApplications() {
    return applications;
  }

  public void setApplications(List<AppYaml> applications) {
    this.applications = applications;
  }

  public void addApplication(AppYaml application) {
    this.applications.add(application);
  }
}
