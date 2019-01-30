package io.harness.k8s.manifest;

import static io.harness.k8s.manifest.ObjectYamlUtils.YAML_DOCUMENT_DELIMITER;
import static io.harness.k8s.manifest.ObjectYamlUtils.splitYamlFile;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.KubernetesResource.redactSecretValues;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.ImmutableSet;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import io.harness.exception.KubernetesValuesException;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ManifestHelper {
  public static final String values_filename = "values.yaml";
  public static final String yaml_file_extension = ".yaml";
  public static final String currentReleaseWorkloadExpression = "${k8s.currentReleaseWorkload}";
  public static final String previousReleaseWorkloadExpression = "${k8s.previousReleaseWorkload}";
  public static final String primaryServiceNameExpression = "${k8s.primaryServiceName}";
  public static final String stageServiceNameExpression = "${k8s.stageServiceName}";

  private static KubernetesResource getKubernetesResourceFromSpec(String spec) {
    Map map = null;

    try {
      YamlReader reader = new YamlReader(spec);
      Object o = reader.read();
      if (o instanceof Map) {
        map = (Map) o;
      } else {
        throw new KubernetesYamlException("Invalid Yaml. Object is not a map.");
      }
    } catch (YamlException e) {
      throw new KubernetesYamlException(e.getMessage(), e.getCause());
    }

    if (!map.containsKey("kind")) {
      throw new KubernetesYamlException("Error processing yaml manifest. kind not found in spec.");
    }

    String kind = map.get("kind").toString();

    if (!map.containsKey("metadata")) {
      throw new KubernetesYamlException("Error processing yaml manifest. metadata not found in spec.");
    }

    Map metadata = (Map) map.get("metadata");
    if (!metadata.containsKey("name")) {
      throw new KubernetesYamlException("Error processing yaml manifest. metadata.name not found in spec.");
    }

    String name = metadata.get("name").toString();

    String namespace = null;
    if (metadata.containsKey("namespace")) {
      namespace = metadata.get("namespace").toString();
    }

    return KubernetesResource.builder()
        .resourceId(KubernetesResourceId.builder().kind(kind).name(name).namespace(namespace).build())
        .value(map)
        .spec(spec)
        .build();
  }

  public static List<KubernetesResource> processYaml(String yamlString) {
    List<String> specs = splitYamlFile(yamlString);

    List<KubernetesResource> resources = new ArrayList<>();

    for (String spec : specs) {
      resources.add(getKubernetesResourceFromSpec(spec));
    }

    return resources;
  }

  public static String toYaml(List<KubernetesResource> resources) {
    StringBuilder stringBuilder = new StringBuilder();

    for (KubernetesResource resource : resources) {
      if (!resource.getSpec().startsWith(YAML_DOCUMENT_DELIMITER)) {
        stringBuilder.append(YAML_DOCUMENT_DELIMITER).append(System.lineSeparator());
      }
      stringBuilder.append(resource.getSpec());
    }

    return stringBuilder.toString();
  }

  public static String toYamlForLogs(List<KubernetesResource> resources) {
    StringBuilder stringBuilder = new StringBuilder();

    for (KubernetesResource resource : resources) {
      String spec = StringUtils.equals(Secret.name(), resource.getResourceId().getKind())
          ? redactSecretValues(resource.getSpec())
          : resource.getSpec();
      if (!spec.startsWith(YAML_DOCUMENT_DELIMITER)) {
        stringBuilder.append(YAML_DOCUMENT_DELIMITER).append(System.lineSeparator());
      }
      stringBuilder.append(spec);
    }

    return stringBuilder.toString();
  }

  private static final Set<String> managedWorkloadKinds = ImmutableSet.of("Deployment", "StatefulSet", "DaemonSet");

  public static List<KubernetesResource> getWorkloads(List<KubernetesResource> resources) {
    return resources.stream()
        .filter(resource -> managedWorkloadKinds.contains(resource.getResourceId().getKind()))
        .filter(resource -> !resource.isDirectApply())
        .collect(Collectors.toList());
  }

  public static KubernetesResource getManagedWorkload(List<KubernetesResource> resources) {
    List<KubernetesResource> result = getWorkloads(resources);
    if (!result.isEmpty()) {
      return result.get(0);
    }
    return null;
  }

  public static KubernetesResource getPrimaryService(List<KubernetesResource> resources) {
    List<KubernetesResource> filteredResources =
        resources.stream().filter(KubernetesResource::isPrimaryService).collect(Collectors.toList());
    if (filteredResources.size() != 1) {
      if (filteredResources.size() > 1) {
        throw new KubernetesYamlException(
            "More than one service is marked Primary. Please specify only one with annotation "
            + HarnessAnnotations.primaryService);
      }
      throw new KubernetesYamlException("No primary service is found in manifests. Please specify one with annotation "
          + HarnessAnnotations.primaryService);
    }
    return filteredResources.get(0);
  }

  public static KubernetesResource getStageService(List<KubernetesResource> resources) {
    List<KubernetesResource> filteredResources =
        resources.stream().filter(KubernetesResource::isStageService).collect(Collectors.toList());
    if (filteredResources.size() != 1) {
      if (filteredResources.size() > 1) {
        throw new KubernetesYamlException(
            "More than one service is marked Stage. Please specify only one with annotation "
            + HarnessAnnotations.stageService);
      }
      throw new KubernetesYamlException("No stage service is found in manifests. Please specify one with annotation "
          + HarnessAnnotations.stageService);
    }
    return filteredResources.get(0);
  }

  public static String getValuesYamlGitFilePath(String filePath) {
    if (isBlank(filePath)) {
      return values_filename;
    }

    return normalizeFolderPath(filePath) + values_filename;
  }

  public static boolean validateValuesFileContents(String valuesFileContent) {
    try {
      if (StringUtils.isBlank(valuesFileContent)) {
        return true;
      }
      YamlReader reader = new YamlReader(valuesFileContent);
      Object o = reader.read();
      if (o instanceof Map) {
        // noop
        return true;
      } else {
        throw new KubernetesValuesException("Object is not a map.");
      }
    } catch (YamlException e) {
      throw new KubernetesValuesException(e.getMessage(), e.getCause());
    }
  }

  public static String normalizeFolderPath(String folderPath) {
    if (isBlank(folderPath)) {
      return folderPath;
    }

    return folderPath.endsWith(File.separator) ? folderPath : folderPath + File.separator;
  }
}
