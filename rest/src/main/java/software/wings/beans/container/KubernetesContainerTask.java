package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import ro.fortsoft.pf4j.util.StringUtils;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactEnumDataProvider;
import software.wings.exception.WingsException;
import software.wings.stencils.EnumData;
import software.wings.utils.KubernetesConvention;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/8/17
 */
@JsonTypeName("KUBERNETES")
public class KubernetesContainerTask extends ContainerTask {
  public static final String DOCKER_IMAGE_NAME_PLACEHOLDER = "hv--docker-image-name--hv";
  public static final String CONTAINER_NAME_PLACEHOLDER = "hv--container-name--hv";
  public static final String SECRET_NAME_PLACEHOLDER = "hv--secret-name--hv";

  @Attributes(title = "LABELS") List<Label> labels;
  private List<ContainerDefinition> containerDefinitions;
  @EnumData(enumDataProvider = ArtifactEnumDataProvider.class) private String artifactName;

  public KubernetesContainerTask() {
    super(DeploymentType.KUBERNETES.name());
  }

  public List<ContainerDefinition> getContainerDefinitions() {
    return containerDefinitions;
  }

  public void setContainerDefinitions(List<ContainerDefinition> containerDefinitions) {
    this.containerDefinitions = containerDefinitions;
  }

  public List<Label> getLabels() {
    return labels;
  }

  public void setLabels(List<Label> labels) {
    this.labels = labels;
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
    @SchemaIgnore private String name;
    @Attributes(title = "Commands") private List<String> commands;
    @Attributes(title = "CPU", required = true) private Integer cpu;
    @Attributes(title = "MEMORY", required = true) private Integer memory;

    @Attributes(title = "PORT MAPPINGS") List<PortMapping> portMappings;
    @Attributes(title = "ENVIRONMENT VARIABLES") List<EnvironmentVariable> environmentVariables;
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

    public List<EnvironmentVariable> getEnvironmentVariables() {
      return environmentVariables;
    }

    public void setEnvironmentVariables(List<EnvironmentVariable> environmentVariables) {
      this.environmentVariables = environmentVariables;
    }
  }

  public static class EnvironmentVariable {
    @Attributes(title = "Environment variable name") private String name;
    @Attributes(title = "Environment variable value") private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class Label {
    @Attributes(title = "Label name") private String name;
    @Attributes(title = "Label value") private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class PortMapping {
    @Attributes(title = "Container port") private Integer containerPort;
    @Attributes(title = "Host port") private Integer hostPort;

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
  public void convertToAdvanced() {
    setAdvancedConfig(fetchYamlConfig());
    setAdvancedType(AdvancedType.YAML);
  }

  public String fetchYamlConfig() {
    try {
      return KubernetesHelper.toYaml(createReplicationController());
    } catch (IOException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", e.getMessage(), e);
    }
  }

  @Override
  public void convertFromAdvanced() {
    setAdvancedConfig("");
  }

  @Override
  public void validateAdvanced() {
    if (StringUtils.isNotEmpty(getAdvancedConfig())) {
      try {
        ReplicationController rc;
        if (getAdvancedType() == AdvancedType.YAML) {
          rc = KubernetesHelper.loadYaml(getAdvancedConfig());
        } else {
          rc = (ReplicationController) KubernetesHelper.loadJson(getAdvancedConfig());
        }
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", e.getMessage(), e);
      }
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Advanced configuration is empty.");
    }
  }

  private ReplicationController createReplicationController() {
    List<Container> containerDefinitions =
        getContainerDefinitions().stream().map(this ::createContainerDefinition).collect(Collectors.toList());

    List<Volume> volumeList = new ArrayList<>();
    getContainerDefinitions().forEach(containerDefinition -> {
      if (containerDefinition.getStorageConfigurations() != null) {
        volumeList.addAll(
            containerDefinition.getStorageConfigurations()
                .stream()
                .map(storageConfiguration
                    -> new VolumeBuilder()
                           .withName(KubernetesConvention.getVolumeName(storageConfiguration.getHostSourcePath()))
                           .withHostPath(new HostPathVolumeSource(storageConfiguration.getHostSourcePath()))
                           .build())
                .collect(Collectors.toList()));
      }
    });

    return new ReplicationControllerBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withNamespace("default")
        .endMetadata()
        .withNewSpec()
        .withReplicas(0)
        .withNewTemplate()
        .withNewMetadata()
        .endMetadata()
        .withNewSpec()
        .addNewImagePullSecret(SECRET_NAME_PLACEHOLDER)
        .addToContainers(containerDefinitions.toArray(new Container[containerDefinitions.size()]))
        .addToVolumes(volumeList.toArray(new Volume[volumeList.size()]))
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  /**
   * Creates container definition
   */
  private Container createContainerDefinition(KubernetesContainerTask.ContainerDefinition wingsContainerDefinition) {
    ContainerBuilder containerBuilder =
        new ContainerBuilder().withName(CONTAINER_NAME_PLACEHOLDER).withImage(DOCKER_IMAGE_NAME_PLACEHOLDER);

    Map<String, Quantity> limits = new HashMap<>();
    if (wingsContainerDefinition.getCpu() != null) {
      limits.put("cpu", new Quantity(wingsContainerDefinition.getCpu().toString()));
    }

    if (wingsContainerDefinition.getMemory() != null) {
      limits.put("memory", new Quantity(wingsContainerDefinition.getMemory() + "Mi"));
    }

    if (!limits.isEmpty()) {
      containerBuilder.withNewResources().withLimits(limits).endResources();
    }

    if (wingsContainerDefinition.getPortMappings() != null) {
      wingsContainerDefinition.getPortMappings().forEach(portMapping
          -> containerBuilder.addNewPort()
                 .withContainerPort(portMapping.getContainerPort())
                 .withHostPort(portMapping.getHostPort())
                 .withProtocol("TCP")
                 .endPort());
    }

    if (wingsContainerDefinition.getCommands() != null) {
      wingsContainerDefinition.getCommands().forEach(command -> {
        if (!command.trim().isEmpty()) {
          containerBuilder.withCommand(command.trim());
        }
      });
    }

    if (wingsContainerDefinition.getEnvironmentVariables() != null) {
      wingsContainerDefinition.getEnvironmentVariables().forEach(
          envVar -> containerBuilder.addNewEnv().withName(envVar.getName()).withValue(envVar.getValue()).endEnv());
    }

    if (wingsContainerDefinition.getLogConfiguration() != null) {
      KubernetesContainerTask.LogConfiguration wingsLogConfiguration = wingsContainerDefinition.getLogConfiguration();
      // TODO:: Check about kubernetes logs.  See https://kubernetes.io/docs/concepts/clusters/logging/
    }

    if (wingsContainerDefinition.getStorageConfigurations() != null) {
      wingsContainerDefinition.getStorageConfigurations().forEach(storageConfiguration
          -> containerBuilder.addNewVolumeMount()
                 .withName(KubernetesConvention.getVolumeName(storageConfiguration.getHostSourcePath()))
                 .withMountPath(storageConfiguration.getContainerPath())
                 .withReadOnly(storageConfiguration.isReadonly())
                 .endVolumeMount());
    }

    return containerBuilder.build();
  }
}
