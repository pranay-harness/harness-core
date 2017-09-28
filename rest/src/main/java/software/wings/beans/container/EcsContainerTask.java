package software.wings.beans.container;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.strip;
import static software.wings.beans.container.ContainerTask.AdvancedType.JSON;

import com.amazonaws.services.ecs.model.HostVolumeProperties;
import com.amazonaws.services.ecs.model.MountPoint;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.amazonaws.services.ecs.model.Volume;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactEnumDataProvider;
import software.wings.exception.WingsException;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.utils.EcsConvention;
import software.wings.utils.JsonUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by anubhaw on 2/6/17.
 */
@JsonTypeName("ECS")
public class EcsContainerTask extends ContainerTask {
  private static final Pattern commentPattern = Pattern.compile("^#.*$");

  @EnumData(enumDataProvider = ArtifactEnumDataProvider.class) private String artifactName;

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

  public String getArtifactName() {
    return artifactName;
  }

  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  @SchemaIgnore
  public String getServiceId() {
    return super.getServiceId();
  }

  public static class ContainerDefinition {
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

  @Override
  public ContainerTask convertToAdvanced() {
    String preamble = "# Enter your Task Definition spec below.\n"
        + "#\n"
        + "# Placeholders:\n"
        + "#\n"
        + "# Required: ${DOCKER_IMAGE_NAME}\n"
        + "#   - Replaced with the Docker image name and tag\n"
        + "#\n"
        + "# Optional: ${CONTAINER_NAME}\n"
        + "#   - Replaced with a container name based on the image name\n"
        + "#\n"
        + "# Harness will set the task family of the task definition.\n"
        + "#\n"
        + "# Service variables will be merged into environment\n"
        + "# variables for all containers, overriding values if\n"
        + "# the name is the same.\n"
        + "#\n"
        + "# ---\n";

    setAdvancedType(JSON);
    setAdvancedConfig(preamble + fetchJsonConfig());
    return this;
  }

  @Override
  public ContainerTask convertFromAdvanced() {
    setAdvancedConfig(null);
    setAdvancedType(null);
    return this;
  }

  public String fetchAdvancedConfigNoComments() {
    if (isNotEmpty(getAdvancedConfig())) {
      StringBuilder strippedConfig = new StringBuilder();
      String[] lines = getAdvancedConfig().split("\n");
      Arrays.stream(lines)
          .filter(line -> !commentPattern.matcher(line).matches())
          .forEach(line -> strippedConfig.append(line).append("\n"));
      return strippedConfig.toString();
    }
    return getAdvancedConfig();
  }

  @Override
  public void validateAdvanced() {
    if (isNotEmpty(getAdvancedConfig())) {
      try {
        String advancedConfig = fetchAdvancedConfigNoComments()
                                    .replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, DUMMY_DOCKER_IMAGE_NAME)
                                    .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, DUMMY_CONTAINER_NAME);
        TaskDefinition taskDefinition = JsonUtils.asObject(advancedConfig, TaskDefinition.class);

        boolean containerHasDockerPlaceholder = taskDefinition.getContainerDefinitions().stream().anyMatch(
            cd -> DUMMY_DOCKER_IMAGE_NAME.equals(cd.getImage()));
        if (!containerHasDockerPlaceholder) {
          throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args",
              "Replication controller spec must have a container definition with "
                  + "${DOCKER_IMAGE_NAME} placeholder.");
        }
      } catch (Exception e) {
        if (e instanceof WingsException) {
          throw(WingsException) e;
        }
        throw new WingsException(
            ErrorCode.INVALID_ARGUMENT, "args", "Cannot create task definition from JSON: " + e.getMessage(), e);
      }
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "ECS advanced configuration is empty.");
    }
  }

  public String fetchJsonConfig() {
    try {
      return JsonUtils.asPrettyJson(createTaskDefinition())
          .replaceAll(DUMMY_DOCKER_IMAGE_NAME, DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_CONTAINER_NAME, CONTAINER_NAME_PLACEHOLDER_REGEX);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", e.getMessage(), e);
    }
  }

  private TaskDefinition createTaskDefinition() {
    Map<String, Volume> volumeMap = new HashMap<>();
    for (EcsContainerTask.ContainerDefinition containerDefinition : getContainerDefinitions()) {
      if (CollectionUtils.isNotEmpty(containerDefinition.getStorageConfigurations())) {
        for (StorageConfiguration storageConfiguration : containerDefinition.getStorageConfigurations()) {
          Volume volume = new Volume();
          String volumeName = EcsConvention.getVolumeName(strip(storageConfiguration.getHostSourcePath()));
          volume.setName(volumeName);
          HostVolumeProperties hostVolumeProperties = new HostVolumeProperties();
          hostVolumeProperties.setSourcePath(strip(storageConfiguration.getHostSourcePath()));
          volume.setHost(hostVolumeProperties);
          volumeMap.put(volume.getName(), volume);
        }
      }
    }

    return new TaskDefinition()
        .withContainerDefinitions(
            getContainerDefinitions()
                .stream()
                .map(containerDefinition
                    -> createContainerDefinition(DUMMY_DOCKER_IMAGE_NAME, DUMMY_CONTAINER_NAME, containerDefinition))
                .collect(toList()))
        .withVolumes(volumeMap.values());
  }

  private com.amazonaws.services.ecs.model.ContainerDefinition createContainerDefinition(
      String imageName, String containerName, EcsContainerTask.ContainerDefinition harnessContainerDefinition) {
    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinition =
        new com.amazonaws.services.ecs.model.ContainerDefinition()
            .withName(strip(containerName))
            .withImage(strip(imageName));

    if (harnessContainerDefinition.getCpu() != null && harnessContainerDefinition.getMemory() > 0) {
      containerDefinition.setCpu(harnessContainerDefinition.getCpu());
    }

    if (harnessContainerDefinition.getMemory() != null && harnessContainerDefinition.getMemory() > 0) {
      containerDefinition.setMemory(harnessContainerDefinition.getMemory());
    }

    if (harnessContainerDefinition.getPortMappings() != null) {
      List<com.amazonaws.services.ecs.model.PortMapping> portMappings =
          harnessContainerDefinition.getPortMappings()
              .stream()
              .map(portMapping
                  -> new com.amazonaws.services.ecs.model.PortMapping()
                         .withContainerPort(portMapping.getContainerPort())
                         .withHostPort(portMapping.getHostPort())
                         .withProtocol(TransportProtocol.Tcp))
              .collect(toList());
      containerDefinition.setPortMappings(portMappings);
    }

    List<String> commands = Optional.ofNullable(harnessContainerDefinition.getCommands())
                                .orElse(emptyList())
                                .stream()
                                .filter(StringUtils::isNotBlank)
                                .map(StringUtils::strip)
                                .collect(toList());
    containerDefinition.setCommand(commands);

    if (harnessContainerDefinition.getLogConfiguration() != null) {
      EcsContainerTask.LogConfiguration harnessLogConfiguration = harnessContainerDefinition.getLogConfiguration();
      if (isNotBlank(harnessLogConfiguration.getLogDriver())) {
        com.amazonaws.services.ecs.model.LogConfiguration logConfiguration =
            new com.amazonaws.services.ecs.model.LogConfiguration().withLogDriver(
                strip(harnessLogConfiguration.getLogDriver()));
        Optional.ofNullable(harnessLogConfiguration.getOptions())
            .orElse(emptyList())
            .forEach(
                logOption -> logConfiguration.addOptionsEntry(strip(logOption.getKey()), strip(logOption.getValue())));
        containerDefinition.setLogConfiguration(logConfiguration);
      }
    }

    if (CollectionUtils.isNotEmpty(harnessContainerDefinition.getStorageConfigurations())) {
      List<StorageConfiguration> harnessStorageConfigurations = harnessContainerDefinition.getStorageConfigurations();
      containerDefinition.setMountPoints(
          harnessStorageConfigurations.stream()
              .map(storageConfiguration
                  -> new MountPoint()
                         .withContainerPath(strip(storageConfiguration.getContainerPath()))
                         .withSourceVolume(EcsConvention.getVolumeName(strip(storageConfiguration.getHostSourcePath())))
                         .withReadOnly(storageConfiguration.isReadonly()))
              .collect(toList()));
    }

    return containerDefinition;
  }
}
