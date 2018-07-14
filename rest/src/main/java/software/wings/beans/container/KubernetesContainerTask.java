package software.wings.beans.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.strip;
import static software.wings.exception.WingsException.USER;
import static software.wings.service.impl.KubernetesHelperService.toYaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactEnumDataProvider;
import software.wings.exception.WingsException;
import software.wings.stencils.EnumData;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brett on 3/8/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("KUBERNETES")
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class KubernetesContainerTask extends ContainerTask {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesContainerTask.class);

  public static final String CONFIG_MAP_NAME_PLACEHOLDER_REGEX = "\\$\\{CONFIG_MAP_NAME}";
  public static final String SECRET_MAP_NAME_PLACEHOLDER_REGEX = "\\$\\{SECRET_MAP_NAME}";

  private static final String SECRET_NAME_PLACEHOLDER_REGEX = "\\$\\{SECRET_NAME}";

  private static final String DUMMY_SECRET_NAME = "hv--secret-name--hv";

  private static final Pattern DAEMON_SET_PATTERN = Pattern.compile("kind:\\s*\"?DaemonSet");
  private static final Pattern STATEFUL_SET_PATTERN = Pattern.compile("kind:\\s*\"?StatefulSet");

  @Attributes(title = "LABELS") private List<Label> labels;
  @EnumData(enumDataProvider = ArtifactEnumDataProvider.class) private String artifactName;

  public KubernetesContainerTask() {
    super(DeploymentType.KUBERNETES.name());
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

  @Override
  public ContainerTask convertToAdvanced() {
    String preamble = "# Enter your Controller YAML spec below.\n"
        + "#\n"
        + "# Supported Controllers:\n"
        + "#   ReplicationController\n"
        + "#   Deployment\n"
        + "#   ReplicaSet\n"
        + "#   StatefulSet\n"
        + "#   DaemonSet\n"
        + "#\n"
        + "# Placeholders:\n"
        + "#\n"
        + "# Required: ${DOCKER_IMAGE_NAME}\n"
        + "#   - Replaced with the Docker image name and tag\n"
        + "#\n"
        + "# Optional: ${CONTAINER_NAME}\n"
        + "#   - Replaced with a container name based on the image name\n"
        + "#\n"
        + "# Optional: ${CONFIG_MAP_NAME}\n"
        + "#   - Replaced with the ConfigMap name (same as controller name)\n"
        + "#     Config map contains all unencrypted service variables and\n"
        + "#     all unencrypted config files, unless a custom\n"
        + "#     config map is provided\n"
        + "#\n"
        + "# Optional: ${SECRET_MAP_NAME}\n"
        + "#   - Replaced with the Secret name (same as controller name)\n"
        + "#     Secret map contains all encrypted service variables and\n"
        + "#     all encrypted config files\n"
        + "#\n"
        + "# Optional: ${SECRET_NAME}\n"
        + "#   - Replaced with the name of the generated image pull\n"
        + "#     secret when pulling from a private Docker registry\n"
        + "#\n"
        + "# Harness will set the controller name, namespace,\n"
        + "# selector labels, and number of replicas.\n"
        + "#\n"
        + "# Service variables will be merged into environment\n"
        + "# variables for all containers, overriding values if\n"
        + "# the name is the same.\n"
        + "#\n"
        + "# ---\n";
    setAdvancedConfig(preamble + fetchYamlConfig());
    return this;
  }

  @Override
  public ContainerTask convertFromAdvanced() {
    setAdvancedConfig(null);
    return this;
  }

  @Override
  public void validateAdvanced() {
    // Instantiating doesn't work when service variable expressions are used so only check for placeholder
    if (isNotEmpty(getAdvancedConfig())) {
      boolean foundImagePlaceholder = false;

      LineIterator lineIterator = new LineIterator(new StringReader(getAdvancedConfig()));
      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (isBlank(line) || line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains("${DOCKER_IMAGE_NAME}")) {
          foundImagePlaceholder = true;
        }
      }
      if (!foundImagePlaceholder) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args",
                "Controller spec must have a container definition with "
                    + "${DOCKER_IMAGE_NAME} placeholder.");
      }
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
          .addParam("args", "Kubernetes advanced configuration is empty.");
    }
  }

  public boolean checkDaemonSet() {
    return isNotBlank(getAdvancedConfig()) && DAEMON_SET_PATTERN.matcher(getAdvancedConfig()).find();
  }

  public boolean checkStatefulSet() {
    return isNotBlank(getAdvancedConfig()) && STATEFUL_SET_PATTERN.matcher(getAdvancedConfig()).find();
  }

  public HasMetadata createController(String containerName, String imageNameTag, String secretName,
      String configMapName, String secretMapName, String domainName) {
    try {
      String configTemplate = isNotBlank(getAdvancedConfig()) ? getAdvancedConfig() : fetchYamlConfig();

      if (isNotEmpty(domainName)) {
        Pattern pattern = ContainerTask.compileRegexPattern(domainName);
        Matcher matcher = pattern.matcher(configTemplate);
        if (!matcher.find()) {
          imageNameTag = domainName + "/" + imageNameTag;
          imageNameTag = imageNameTag.replaceAll("//", "/");
        }
      }

      String controllerYaml = configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageNameTag)
                                  .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName)
                                  .replaceAll(SECRET_NAME_PLACEHOLDER_REGEX, secretName)
                                  .replaceAll(CONFIG_MAP_NAME_PLACEHOLDER_REGEX, configMapName)
                                  .replaceAll(SECRET_MAP_NAME_PLACEHOLDER_REGEX, secretMapName);
      HasMetadata controller = KubernetesHelper.loadYaml(controllerYaml);
      if (controller == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse Controller YAML: " + controllerYaml);
      }
      return controller;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", Misc.getMessage(e));
    }
  }

  private String fetchYamlConfig() {
    try {
      return toYaml(createDeployment())
          .replaceAll(DUMMY_DOCKER_IMAGE_NAME, DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_CONTAINER_NAME, CONTAINER_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_SECRET_NAME, SECRET_NAME_PLACEHOLDER_REGEX);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", Misc.getMessage(e));
    }
  }

  private Deployment createDeployment() {
    Map<String, Volume> volumeMap = new HashMap<>();
    for (ContainerDefinition containerDefinition : getContainerDefinitions()) {
      if (containerDefinition.getStorageConfigurations() != null) {
        for (StorageConfiguration storageConfiguration : containerDefinition.getStorageConfigurations()) {
          if (isNotBlank(storageConfiguration.getHostSourcePath())) {
            String volumeName = KubernetesConvention.getVolumeName(strip(storageConfiguration.getHostSourcePath()));
            volumeMap.put(volumeName,
                new VolumeBuilder()
                    .withName(volumeName)
                    .withHostPath(new HostPathVolumeSource(strip(storageConfiguration.getHostSourcePath())))
                    .build());
          }
        }
      }
    }

    return new DeploymentBuilder()
        .withNewMetadata()
        .endMetadata()
        .withNewSpec()
        .withReplicas(0)
        .withNewTemplate()
        .withNewMetadata()
        .endMetadata()
        .withNewSpec()
        .addNewImagePullSecret(DUMMY_SECRET_NAME)
        .addToContainers(
            getContainerDefinitions().stream().map(this ::createContainerDefinition).toArray(Container[] ::new))
        .addToVolumes(volumeMap.values().toArray(new Volume[0]))
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  /**
   * Creates container definition
   */
  private Container createContainerDefinition(ContainerDefinition wingsContainerDefinition) {
    ContainerBuilder containerBuilder =
        new ContainerBuilder().withName(DUMMY_CONTAINER_NAME).withImage(DUMMY_DOCKER_IMAGE_NAME);

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

    if (wingsContainerDefinition.getLogConfiguration() != null) {
      LogConfiguration wingsLogConfiguration = wingsContainerDefinition.getLogConfiguration();
      // TODO:: Check about kubernetes logs.  See https://kubernetes.io/docs/concepts/clusters/logging/
    }

    if (wingsContainerDefinition.getStorageConfigurations() != null) {
      wingsContainerDefinition.getStorageConfigurations()
          .stream()
          .filter(storageConfiguration
              -> isNotBlank(storageConfiguration.getHostSourcePath())
                  && isNotBlank(storageConfiguration.getContainerPath()))
          .forEach(storageConfiguration
              -> containerBuilder.addNewVolumeMount()
                     .withName(KubernetesConvention.getVolumeName(strip(storageConfiguration.getHostSourcePath())))
                     .withMountPath(strip(storageConfiguration.getContainerPath()))
                     .withReadOnly(storageConfiguration.isReadonly())
                     .endVolumeMount());
    }

    return containerBuilder.build();
  }

  public ContainerTask cloneInternal() {
    ContainerTask newContainerTask = new KubernetesContainerTask();
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
