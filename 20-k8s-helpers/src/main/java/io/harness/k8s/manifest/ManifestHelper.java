package io.harness.k8s.manifest;

import com.esotericsoftware.yamlbeans.YamlReader;
import io.harness.exception.KubernetesYamlException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManifestHelper {
  public static List<KubernetesResource> processYaml(String yamlString) {
    YamlReader reader = new YamlReader(yamlString);

    List<KubernetesResource> resources = new ArrayList<>();

    try {
      // A YAML can contain more than one YAML document.
      // Call to YamlReader.read() deserializes the next document into an object.
      // YAML documents are delimited by "---"
      while (true) {
        Map map = (Map) reader.read();
        if (map == null) {
          break;
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

        resources.add(KubernetesResource.builder()
                          .resourceId(KubernetesResourceId.builder().kind(kind).name(name).namespace(namespace).build())
                          .value(map)
                          .build());
      }
    } catch (KubernetesYamlException e) {
      throw e;
    } catch (Exception e) {
      throw new KubernetesYamlException("Error processing yaml manifest", e);
    }

    return resources;
  }
}
