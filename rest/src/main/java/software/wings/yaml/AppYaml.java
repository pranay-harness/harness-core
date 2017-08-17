package software.wings.yaml;

import software.wings.beans.Service;

import java.util.ArrayList;
import java.util.List;

public class AppYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public List<String> services = new ArrayList<>();

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

  public List<String> getServiceNames() {
    return services;
  }

  public void setServiceNames(List<String> serviceNames) {
    this.services = serviceNames;
  }

  public void setServices(List<Service> services) {
    for (Service service : services) {
      this.services.add(service.getName());
    }
  }

  public void addService(String serviceName) {
    this.services.add(serviceName);
  }
}
