package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.DeploymentType;
import software.wings.stencils.DefaultValue;

import java.util.List;

/**
 * Created by anubhaw on 2/6/17.
 */
@JsonTypeName("ECS")
public class EcsContainerTask extends ContainerTask {
  private List<ContainerDefinition> containerDefinitions;

  public EcsContainerTask() {
    super(DeploymentType.ECS.name());
  }

  public List<ContainerDefinition> getContainerDefinitions() {
    return containerDefinitions;
  }

  public void setContainerDefinitions(List<ContainerDefinition> containerDefinitions) {
    this.containerDefinitions = containerDefinitions;
  }

  @SchemaIgnore
  public String getServiceId() {
    return super.getServiceId();
  }

  public static class ContainerDefinition {
    @SchemaIgnore private String name;
    @Attributes(title = "Commands") private List<String> commands;
    @Attributes(title = "CPU") private Integer cpu;
    @DefaultValue("256") @Attributes(title = "MEMORY") private Integer memory;

    @Attributes(title = "PORT MAPPINGS") List<PortMapping> portMappings;
    @Attributes(title = "LOG CONFIGURATION") @SchemaIgnore private LogConfiguration logConfiguration;
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
  }

  public static class PortMapping {
    @Attributes(title = "Container port") private Integer containerPort;
    @Attributes(title = "Host port") private Integer hostPort;
    @Attributes(title = "Expose on Load Balancer") private boolean loadBalancerPort;

    public Integer getContainerPort() {
      return containerPort;
    }

    public void setContainerPort(Integer containerPort) {
      this.containerPort = containerPort;
    }

    public Integer getHostPort() {
      return hostPort;
    }

    public void setHostPort(Integer hostPort) {
      this.hostPort = hostPort;
    }

    /**
     * Getter for property 'loadBalancerPort'.
     *
     * @return Value for property 'loadBalancerPort'.
     */
    public boolean isLoadBalancerPort() {
      return loadBalancerPort;
    }

    /**
     * Setter for property 'loadBalancerPort'.
     *
     * @param loadBalancerPort Value to set for property 'loadBalancerPort'.
     */
    public void setLoadBalancerPort(boolean loadBalancerPort) {
      this.loadBalancerPort = loadBalancerPort;
    }
  }

  public static class LogConfiguration {
    @Attributes(title = "Log Driver") private String logDriver;
    @Attributes(title = "Options") private List<LogOption> options;

    public String getLogDriver() {
      return logDriver;
    }

    public void setLogDriver(String logDriver) {
      this.logDriver = logDriver;
    }

    public List<LogOption> getOptions() {
      return options;
    }

    public void setOptions(List<LogOption> options) {
      this.options = options;
    }

    public static class LogOption {
      private String key;
      private String value;

      public String getKey() {
        return key;
      }

      public void setKey(String key) {
        this.key = key;
      }

      public String getValue() {
        return value;
      }

      public void setValue(String value) {
        this.value = value;
      }
    }
  }

  public static class StorageConfiguration {
    @Attributes(title = "Host Source Path") private String hostSourcePath;
    @Attributes(title = "Container Path") private String containerPath;
    @Attributes(title = "Options") private boolean readonly = false;

    public String getHostSourcePath() {
      return hostSourcePath;
    }

    public void setHostSourcePath(String hostSourcePath) {
      this.hostSourcePath = hostSourcePath;
    }

    public String getContainerPath() {
      return containerPath;
    }

    public void setContainerPath(String containerPath) {
      this.containerPath = containerPath;
    }

    public boolean isReadonly() {
      return readonly;
    }

    public void setReadonly(boolean readonly) {
      this.readonly = readonly;
    }
  }
}
