package software.wings.yaml;

import software.wings.beans.Application;

import java.util.ArrayList;
import java.util.List;

public class OrderIndentTestYaml extends BaseYaml {
  private String name;
  private String description;
  private List<Application.Yaml> applications = new ArrayList<>();

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

  public List<Application.Yaml> getApplications() {
    return applications;
  }

  public void setApplications(List<Application.Yaml> applications) {
    this.applications = applications;
  }

  public void addApplication(Application.Yaml application) {
    this.applications.add(application);
  }
}
