package software.wings.yaml;

import software.wings.beans.Service;
import software.wings.beans.command.ServiceCommand;

import java.util.ArrayList;
import java.util.List;

public class ServiceYaml extends GenericYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public String artifactType;
  @YamlSerialize public List<String> serviceCommands = new ArrayList<String>();

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

  public String getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(String artifactType) {
    this.artifactType = artifactType;
  }

  public List<String> getServiceCommandNames() {
    return serviceCommands;
  }

  public void setServiceCommandNames(List<String> serviceCommandNames) {
    this.serviceCommands = serviceCommandNames;
  }

  public void setServiceCommands(List<ServiceCommand> serviceCommands) {
    for (ServiceCommand serviceCommand : serviceCommands) {
      this.serviceCommands.add(serviceCommand.getName());
    }
  }

  public void addServiceCommand(String serviceCommandName) {
    this.serviceCommands.add(serviceCommandName);
  }
}