package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.VariableProcessor;

import java.util.ArrayList;
import java.util.List;

public class ServiceYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private VariableProcessor variableProcessor = new VariableProcessor();

  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public String artifactType;
  @YamlSerialize public List<String> serviceCommands = new ArrayList<String>();
  @YamlSerialize public List<ConfigVarYaml> configVariables = new ArrayList<ConfigVarYaml>();

  public ServiceYaml() {}

  public ServiceYaml(Service service) {
    this.name = service.getName();
    this.description = service.getDescription();
    this.artifactType = service.getArtifactType().toString();
  }

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

  public void setServiceCommands(List<String> serviceCommandNames) {
    this.serviceCommands = serviceCommandNames;
  }

  public void setServiceCommandNamesFromServiceCommands(List<ServiceCommand> serviceCommands) {
    for (ServiceCommand serviceCommand : serviceCommands) {
      this.serviceCommands.add(serviceCommand.getName());
    }
  }

  public void addServiceCommand(String serviceCommandName) {
    this.serviceCommands.add(serviceCommandName);
  }

  public List<ConfigVarYaml> getConfigVariables() {
    return configVariables;
  }

  public void setConfigVariables(List<ConfigVarYaml> configVariables) {
    this.configVariables = configVariables;
  }

  public void setConfigVariablesFromServiceVariables(List<ServiceVariable> serviceVariables) {
    for (ServiceVariable serviceVariable : serviceVariables) {
      // String configVar = serviceVariable.getName() + ": " + new String(serviceVariable.getValue());
      ConfigVarYaml configVar = new ConfigVarYaml(serviceVariable.getName(), new String(serviceVariable.getValue()));
      this.configVariables.add(configVar);
    }
  }

  public void addConfigVariable(ConfigVarYaml configVar) {
    this.configVariables.add(configVar);
  }
}