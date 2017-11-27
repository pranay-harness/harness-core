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
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactEnumDataProvider;
import software.wings.exception.WingsException;
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

  public EcsContainerTask() {
    super(DeploymentType.ECS.name());
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
    for (ContainerDefinition containerDefinition : getContainerDefinitions()) {
      if (CollectionUtils.isNotEmpty(containerDefinition.getStorageConfigurations())) {
        for (StorageConfiguration storageConfiguration : containerDefinition.getStorageConfigurations()) {
          if (isNotBlank(storageConfiguration.getHostSourcePath())) {
            String volumeName = EcsConvention.getVolumeName(strip(storageConfiguration.getHostSourcePath()));
            Volume volume = new Volume();
            volume.setName(volumeName);
            HostVolumeProperties hostVolumeProperties = new HostVolumeProperties();
            hostVolumeProperties.setSourcePath(strip(storageConfiguration.getHostSourcePath()));
            volume.setHost(hostVolumeProperties);
            volumeMap.put(volume.getName(), volume);
          }
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
      String imageName, String containerName, ContainerDefinition harnessContainerDefinition) {
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
      LogConfiguration harnessLogConfiguration = harnessContainerDefinition.getLogConfiguration();
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Builder
  public static class Yaml extends ContainerTask.Yaml {
    public Yaml() {}
  }
}
