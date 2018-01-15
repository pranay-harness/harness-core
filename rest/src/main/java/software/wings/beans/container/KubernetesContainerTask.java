package software.wings.beans.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.strip;
import static software.wings.beans.container.ContainerTask.AdvancedType.JSON;
import static software.wings.beans.container.ContainerTask.AdvancedType.YAML;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactEnumDataProvider;
import software.wings.exception.WingsException;
import software.wings.stencils.EnumData;
import software.wings.utils.KubernetesConvention;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 3/8/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("KUBERNETES")
public class KubernetesContainerTask extends ContainerTask {
  private static final Logger logger = LoggerFactory.getLogger(KubernetesContainerTask.class);

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
    String preamble = "# Enter your Controller spec below.\n"
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
        + "#\n";

    setAdvancedType(YAML);
    setAdvancedConfig(preamble + fetchYamlConfig());
    return this;
  }

  @Override
  public ContainerTask convertFromAdvanced() {
    setAdvancedConfig(null);
    setAdvancedType(null);
    return this;
  }

  @Override
  public void validateAdvanced() {
    if (isNotEmpty(getAdvancedConfig())) {
      try {
        String advancedConfig = getAdvancedConfig()
                                    .replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, DUMMY_DOCKER_IMAGE_NAME)
                                    .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, DUMMY_CONTAINER_NAME)
                                    .replaceAll(SECRET_NAME_PLACEHOLDER_REGEX, DUMMY_SECRET_NAME);
        HasMetadata controller;
        if (getAdvancedType() == YAML) {
          controller = KubernetesHelper.loadYaml(advancedConfig);
        } else {
          controller = (HasMetadata) KubernetesHelper.loadJson(advancedConfig);
        }

        PodTemplateSpec podTemplateSpec = null;

        if (controller instanceof ReplicationController) {
          podTemplateSpec = ((ReplicationController) controller).getSpec().getTemplate();
        } else if (controller instanceof Deployment) {
          podTemplateSpec = ((Deployment) controller).getSpec().getTemplate();
        } else if (controller instanceof ReplicaSet) {
          podTemplateSpec = ((ReplicaSet) controller).getSpec().getTemplate();
        } else if (controller instanceof StatefulSet) {
          podTemplateSpec = ((StatefulSet) controller).getSpec().getTemplate();
        } else if (controller instanceof DaemonSet) {
          podTemplateSpec = ((DaemonSet) controller).getSpec().getTemplate();
        }

        if (podTemplateSpec == null || podTemplateSpec.getMetadata() == null) {
          throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Missing valid pod template.");
        }

        boolean containerHasDockerPlaceholder = false;
        try {
          containerHasDockerPlaceholder = podTemplateSpec.getSpec().getContainers().stream().anyMatch(
              container -> DUMMY_DOCKER_IMAGE_NAME.equals(container.getImage()));
        } catch (Exception e) {
          logger.error("Controller spec must have a container definition with ${DOCKER_IMAGE_NAME} placeholder.", e);
        }
        if (!containerHasDockerPlaceholder) {
          throw new WingsException(ErrorCode.INVALID_ARGUMENT)
              .addParam("args",
                  "Controller spec must have a container definition with "
                      + "${DOCKER_IMAGE_NAME} placeholder.");
        }
      } catch (Exception e) {
        if (e instanceof WingsException) {
          throw(WingsException) e;
        }
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, e)
            .addParam("args", "Cannot create controller from " + getAdvancedType().name() + ": " + e.getMessage());
      }
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Configuration is empty.");
    }
  }

  public Class<? extends HasMetadata> kubernetesType() {
    return createController(DUMMY_CONTAINER_NAME, DUMMY_DOCKER_IMAGE_NAME, DUMMY_SECRET_NAME).getClass();
  }

  public HasMetadata createController(String containerName, String imageNameTag, String secretName) {
    String configTemplate;
    AdvancedType type;
    if (isNotEmpty(getAdvancedConfig())) {
      configTemplate = getAdvancedConfig();
      type = getAdvancedType();
    } else {
      configTemplate = fetchYamlConfig();
      type = YAML;
    }

    String config = configTemplate.replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageNameTag)
                        .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName)
                        .replaceAll(SECRET_NAME_PLACEHOLDER_REGEX, secretName);

    try {
      return type == JSON ? (HasMetadata) KubernetesHelper.loadJson(config) : KubernetesHelper.loadYaml(config);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", e.getMessage());
    }
  }

  private String fetchYamlConfig() {
    try {
      return KubernetesHelper.toYaml(createReplicationController())
          .replaceAll(DUMMY_DOCKER_IMAGE_NAME, DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_CONTAINER_NAME, CONTAINER_NAME_PLACEHOLDER_REGEX)
          .replaceAll(DUMMY_SECRET_NAME, SECRET_NAME_PLACEHOLDER_REGEX);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", e.getMessage());
    }
  }

  private ReplicationController createReplicationController() {
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

    return new ReplicationControllerBuilder()
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
        .addToVolumes(volumeMap.values().toArray(new Volume[volumeMap.size()]))
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ContainerTask.Yaml {
    @Builder
    public Yaml(String type, String harnessApiVersion, String advancedType, String advancedConfig,
        ContainerDefinition.Yaml containerDefinition) {
      super(type, harnessApiVersion, advancedType, advancedConfig, containerDefinition);
    }
  }
}
