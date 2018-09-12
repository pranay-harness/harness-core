package software.wings.beans.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.strip;

import com.amazonaws.services.ecs.model.HostVolumeProperties;
import com.amazonaws.services.ecs.model.MountPoint;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.amazonaws.services.ecs.model.TransportProtocol;
import com.amazonaws.services.ecs.model.Volume;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.DeploymentType;
import software.wings.beans.artifact.ArtifactEnumDataProvider;
import software.wings.stencils.EnumData;
import software.wings.utils.EcsConvention;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by anubhaw on 2/6/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ECS")
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class EcsContainerTask extends ContainerTask {
  static final String DUMMY_EXECUTION_ROLE_ARN = "hv--execution-role--hv";
  static final String EXECUTION_ROLE_PLACEHOLDER_REGEX = "\\$\\{EXECUTION_ROLE}";
  private static final Pattern commentPattern = Pattern.compile("^#.*$");
  public static final Integer DEFAULT_CONTAINER_DEFINITION_MEMORY = 1024;

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
    String preamble = "# Enter your Task Definition JSON spec below.\n"
        + "#\n"
        + "# Placeholders:\n"
        + "#\n"
        + "# Required: ${DOCKER_IMAGE_NAME}\n"
        + "#   - Replaced with the Docker image name and tag\n"
        + "#\n"
        + "# Optional: ${CONTAINER_NAME}\n"
        + "#   - Replaced with a container name based on the image name\n"
        + "#\n"
        + "# Required For Fargate: ${EXECUTION_ROLE}\n"
        + "#   - Replaced with execution role arn\n"
        + "#\n"
        + "# Harness will set the task family of the task definition.\n"
        + "#\n"
        + "# Service variables will be merged into environment\n"
        + "# variables for all containers, overriding values if\n"
        + "# the name is the same.\n"
        + "#\n"
        + "# ---\n";
    setAdvancedConfig(preamble + fetchJsonConfig());
    return this;
  }

  @Override
  public ContainerTask convertFromAdvanced() {
    setAdvancedConfig(null);
    return this;
  }

  public String fetchAdvancedConfigNoComments() {
    if (isNotEmpty(getAdvancedConfig())) {
      return Arrays.stream(getAdvancedConfig().split("\n"))
          .filter(line -> !commentPattern.matcher(line).matches())
          .collect(joining("\n"));
    }
    return getAdvancedConfig();
  }

  @Override
  public void validateAdvanced() {
    // Instantiating doesn't work when service variable expressions are used so only check for placeholder
    if (isNotEmpty(getAdvancedConfig())) {
      boolean foundImagePlaceholder = false;

      LineIterator lineIterator = new LineIterator(new StringReader(getAdvancedConfig()));
      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains("${DOCKER_IMAGE_NAME}")) {
          foundImagePlaceholder = true;
        }
      }
      if (!foundImagePlaceholder) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args",
                "Task definition spec must have a container definition with "
                    + "${DOCKER_IMAGE_NAME} placeholder.");
      }
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
          .addParam("args", "ECS advanced configuration is empty.");
    }
  }

  public TaskDefinition createTaskDefinition(
      String containerName, String imageName, String executionRole, String domainName) {
    String configTemplate;
    if (isNotEmpty(getAdvancedConfig())) {
      configTemplate = fetchAdvancedConfigNoComments();
    } else {
      configTemplate = fetchJsonConfig();
    }

    if (executionRole == null) {
      executionRole = "null";
    }

    if (isNotEmpty(domainName)) {
      Pattern pattern = ContainerTask.compileRegexPattern(domainName);
      Matcher matcher = pattern.matcher(configTemplate);
      if (!matcher.find()) {
        imageName = domainName + "/" + imageName;
        imageName = imageName.replaceAll("//", "/");
      }
    }

    String config = configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageName)
                        .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName)
                        .replaceAll(EXECUTION_ROLE_PLACEHOLDER_REGEX, executionRole);

    return JsonUtils.asObject(config, TaskDefinition.class);
  }

  private String fetchJsonConfig() {
    try {
      return JsonUtils.asPrettyJson(createTaskDefinition())
          .replaceAll(DUMMY_DOCKER_IMAGE_NAME, DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_CONTAINER_NAME, CONTAINER_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_EXECUTION_ROLE_ARN, EXECUTION_ROLE_PLACEHOLDER_REGEX);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", Misc.getMessage(e));
    }
  }

  private TaskDefinition createTaskDefinition() {
    Map<String, Volume> volumeMap = new HashMap<>();
    for (ContainerDefinition containerDefinition : getContainerDefinitions()) {
      if (isNotEmpty(containerDefinition.getStorageConfigurations())) {
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
        .withExecutionRoleArn(DUMMY_EXECUTION_ROLE_ARN)
        .withVolumes(volumeMap.values())
        .withCpu(getContainerDefinitions()
                     .stream()
                     .filter(def -> def.getCpu() != null)
                     .findFirst()
                     .map(cd -> cd.getCpu().toString())
                     .orElse(null))
        .withMemory(getContainerDefinitions()
                        .stream()
                        .filter(def -> def.getMemory() != null)
                        .findFirst()
                        .map(cd -> cd.getMemory().toString())
                        .orElse(null));
  }

  public com.amazonaws.services.ecs.model.ContainerDefinition createContainerDefinition(
      String imageName, String containerName, ContainerDefinition harnessContainerDefinition) {
    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinition =
        new com.amazonaws.services.ecs.model.ContainerDefinition()
            .withName(strip(containerName))
            .withImage(strip(imageName));

    if (harnessContainerDefinition.getMemory() != null && harnessContainerDefinition.getMemory() > 0) {
      containerDefinition.setMemory(harnessContainerDefinition.getMemory());
    } else {
      // Memory can not be null, so setting to default value of 1024 in advanced config.
      containerDefinition.setMemory(DEFAULT_CONTAINER_DEFINITION_MEMORY);
    }

    if (harnessContainerDefinition.getCpu() != null) {
      containerDefinition.setCpu(harnessContainerDefinition.getCpu());
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

    if (isNotEmpty(harnessContainerDefinition.getStorageConfigurations())) {
      List<StorageConfiguration> harnessStorageConfigurations = harnessContainerDefinition.getStorageConfigurations();
      containerDefinition.setMountPoints(
          harnessStorageConfigurations.stream()
              .filter(storageConfiguration -> isNotBlank(storageConfiguration.getContainerPath()))
              .map(storageConfiguration
                  -> new MountPoint()
                         .withContainerPath(strip(storageConfiguration.getContainerPath()))
                         .withSourceVolume(EcsConvention.getVolumeName(strip(storageConfiguration.getHostSourcePath())))
                         .withReadOnly(storageConfiguration.isReadonly()))
              .collect(toList()));
    }

    return containerDefinition;
  }

  public ContainerTask cloneInternal() {
    ContainerTask newContainerTask = new EcsContainerTask();
    copyConfigToContainerTask(newContainerTask);
    return newContainerTask;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ContainerTask.Yaml {
    @Builder
    public Yaml(
        String type, String harnessApiVersion, String advancedConfig, ContainerDefinition.Yaml containerDefinition) {
      super(type, harnessApiVersion, advancedConfig, containerDefinition);
    }
  }
}
