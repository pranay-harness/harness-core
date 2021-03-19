package software.wings.beans.container;

import software.wings.stencils.DefaultValue;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Builder;

@Builder
public class ContainerDefinition {
  @Attributes(title = "PORT MAPPINGS") List<PortMapping> portMappings;
  @SchemaIgnore private String name;
  @Attributes(title = "Commands") private List<String> commands;
  @Attributes(title = "CPU", required = true) private Double cpu;
  @DefaultValue("256") @Attributes(title = "MEMORY", required = true) private Integer memory;
  @Attributes(title = "LOG CONFIGURATION") private LogConfiguration logConfiguration;
  @Attributes(title = "STORAGE/VOLUME") private List<StorageConfiguration> storageConfigurations;

  @SchemaIgnore
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getCommands() {
    return commands;
  }

  public void setCommands(List<String> commands) {
    this.commands = commands;
  }

  public LogConfiguration getLogConfiguration() {
    return logConfiguration;
  }

  public void setLogConfiguration(LogConfiguration logConfiguration) {
    this.logConfiguration = logConfiguration;
  }

  public List<StorageConfiguration> getStorageConfigurations() {
    return storageConfigurations;
  }

  public void setStorageConfigurations(List<StorageConfiguration> storageConfigurations) {
    this.storageConfigurations = storageConfigurations;
  }

  public Double getCpu() {
    return cpu;
  }

  public void setCpu(Double cpu) {
    this.cpu = cpu;
  }

  public Integer getMemory() {
    return memory;
  }

  public void setMemory(Integer memory) {
    this.memory = memory;
  }

  public List<PortMapping> getPortMappings() {
    return portMappings;
  }

  public void setPortMappings(List<PortMapping> portMappings) {
    this.portMappings = portMappings;
  }
}
