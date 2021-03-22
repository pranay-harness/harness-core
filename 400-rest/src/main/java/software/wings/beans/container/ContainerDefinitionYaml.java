package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ContainerDefinitionYaml extends BaseYaml {
  List<PortMappingYaml> portMappings;
  private String name;
  private List<String> commands;
  private Double cpu;
  private Integer memory;
  private LogConfigurationYaml logConfiguration;
  private List<StorageConfiguration.Yaml> storageConfigurations;

  @Builder
  public ContainerDefinitionYaml(List<PortMappingYaml> portMappings, String name, List<String> commands, Double cpu,
      Integer memory, LogConfigurationYaml logConfiguration, List<StorageConfiguration.Yaml> storageConfigurations) {
    this.portMappings = portMappings;
    this.name = name;
    this.commands = commands;
    this.cpu = cpu;
    this.memory = memory;
    this.logConfiguration = logConfiguration;
    this.storageConfigurations = storageConfigurations;
  }
}
