package io.harness.k8s.manifest;

import static io.harness.k8s.manifest.ObjectYamlUtils.YAML_DOCUMENT_DELIMITER;
import static io.harness.k8s.manifest.ObjectYamlUtils.splitYamlFile;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.KubernetesResource.redactSecretValues;

import com.google.common.collect.ImmutableSet;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ManifestHelper {
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

  public static KubernetesResourceId getManagedWorkload(List<KubernetesResource> resources) {
    List<KubernetesResource> result =
        resources.stream()
            .filter(resource -> managedWorkloadKinds.contains(resource.getResourceId().getKind()))
            .filter(resource -> !resource.isDirectApply())
            .collect(Collectors.toList());

    return result.get(0).getResourceId().cloneInternal();
  }
}
