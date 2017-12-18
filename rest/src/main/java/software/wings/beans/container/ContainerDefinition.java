package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.stencils.DefaultValue;
import software.wings.yaml.BaseYaml;

import java.util.List;

@Builder
public class ContainerDefinition {
  @Attributes(title = "PORT MAPPINGS") List<PortMapping> portMappings;
  @SchemaIgnore private String name;
  @Attributes(title = "Commands") private List<String> commands;
  @Attributes(title = "CPU", required = true) private Integer cpu;
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

  public Integer getCpu() {
    return cpu;
  }

  public void setCpu(Integer cpu) {
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYaml {
    List<PortMapping.Yaml> portMappings;
    private String name;
    private List<String> commands;
    private Integer cpu;
    private Integer memory;
    private LogConfiguration.Yaml logConfiguration;
    private List<StorageConfiguration.Yaml> storageConfigurations;

    @Builder
    public Yaml(List<PortMapping.Yaml> portMappings, String name, List<String> commands, Integer cpu, Integer memory,
        LogConfiguration.Yaml logConfiguration, List<StorageConfiguration.Yaml> storageConfigurations) {
      this.portMappings = portMappings;
      this.name = name;
      this.commands = commands;
      this.cpu = cpu;
      this.memory = memory;
      this.logConfiguration = logConfiguration;
      this.storageConfigurations = storageConfigurations;
    }
  }
}