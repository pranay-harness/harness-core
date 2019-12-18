package io.harness.k8s.manifest;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static io.harness.k8s.manifest.ObjectYamlUtils.YAML_DOCUMENT_DELIMITER;
import static io.harness.k8s.manifest.ObjectYamlUtils.newLineRegex;
import static io.harness.k8s.manifest.ObjectYamlUtils.splitYamlFile;
import static io.harness.k8s.model.Kind.Secret;
import static io.harness.k8s.model.KubernetesResource.redactSecretValues;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.ImmutableSet;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.parser.Parser.ParserException;
import com.esotericsoftware.yamlbeans.tokenizer.Tokenizer.TokenizerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import io.harness.exception.KubernetesValuesException;
import io.harness.exception.KubernetesYamlException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ManifestHelper {
  public static final String values_filename = "values.yaml";
  public static final String yaml_file_extension = ".yaml";
  public static final String yml_file_extension = ".yml";
  public static final String currentReleaseWorkloadExpression = "${k8s.currentReleaseWorkload}";
  public static final String previousReleaseWorkloadExpression = "${k8s.previousReleaseWorkload}";
  public static final String CREATION_TIMESTAMP = "creationTimestamp";
  public static final String METADATA = "metadata";
  public static final String DELETION_TIMESTAMP = "deletionTimestamp";

  private static final String VALUES_EXPRESSION = ".Values";
  public static final int MAX_VALUES_EXPRESSION_RECURSION_DEPTH = 10;

  public static KubernetesResource getKubernetesResourceFromSpec(String spec) {
    Map map = null;

    try {
      YamlReader reader = new YamlReader(spec);
      Object o = reader.read();
      if (o == null) {
        return null;
      }
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

    if (!map.containsKey(METADATA)) {
      throw new KubernetesYamlException("Error processing yaml manifest. metadata not found in spec.");
    }

    Map metadata = (Map) map.get(METADATA);
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
        .spec(removeNullTimeStampFromSpec(spec))
        .build();
  }

  public static String removeNullTimeStampFromSpec(String spec) {
    Map<String, Object> yamlMap;
    try {
      ObjectMapper mapper =
          new ObjectMapper(new YAMLFactory().enable(Feature.MINIMIZE_QUOTES).configure(WRITE_DOC_START_MARKER, false));
      yamlMap = (Map<String, Object>) mapper.readValue(spec, Map.class);
      Set<String> workloads =
          ImmutableSet.of("Deployment", "StatefulSet", "DaemonSet", "Service", "Job", "Secret", "ConfigMap", "Pod");

      String kind = yamlMap.get("kind").toString();
      if (!workloads.contains(kind)) {
        return spec;
      }

      boolean updateNeeded = false;
      Map metadata = (Map) yamlMap.get(METADATA);
      if (metadata.containsKey(CREATION_TIMESTAMP) && metadata.get(CREATION_TIMESTAMP) == null) {
        metadata.remove(CREATION_TIMESTAMP);
        updateNeeded = true;
      }
      if (metadata.containsKey(DELETION_TIMESTAMP) && metadata.get(DELETION_TIMESTAMP) == null) {
        metadata.remove(DELETION_TIMESTAMP);
        updateNeeded = true;
      }

      if (yamlMap.containsKey("spec")) {
        Map specMap = (Map) yamlMap.get("spec");
        if (specMap != null && specMap.containsKey("template")) {
          Map templateMap = (Map) specMap.get("template");
          if (templateMap != null && templateMap.containsKey("metadata")) {
            Map metaDataMap = (Map) templateMap.get("metadata");
            if (metaDataMap != null) {
              if (metaDataMap.containsKey(CREATION_TIMESTAMP) && metaDataMap.get(CREATION_TIMESTAMP) == null) {
                metaDataMap.remove(CREATION_TIMESTAMP);
                updateNeeded = true;
              }

              if (metaDataMap.containsKey(DELETION_TIMESTAMP) && metaDataMap.get(DELETION_TIMESTAMP) == null) {
                metaDataMap.remove(DELETION_TIMESTAMP);
                updateNeeded = true;
              }
            }
          }
        }
      }

      if (updateNeeded) {
        spec = mapper.writeValueAsString(yamlMap);
      }
    } catch (Exception e) {
      logger.warn("Exception in removing null value createTimestamp and deleteTimestamp", e);
    }

    return spec;
  }

  public static List<KubernetesResource> processYaml(String yamlString) {
    List<String> specs = splitYamlFile(yamlString);

    List<KubernetesResource> resources = new ArrayList<>();

    for (String spec : specs) {
      KubernetesResource resourceFromSpec = getKubernetesResourceFromSpec(spec);
      if (resourceFromSpec != null) {
        resources.add(resourceFromSpec);
      }
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

  public static List<KubernetesResource> getWorkloadsForCanary(List<KubernetesResource> resources) {
    return resources.stream()
        .filter(resource -> ImmutableSet.of(Kind.Deployment.name()).contains(resource.getResourceId().getKind()))
        .filter(resource -> !resource.isDirectApply())
        .collect(Collectors.toList());
  }

  public static List<KubernetesResource> getWorkloadsForApplyState(List<KubernetesResource> resources) {
    return resources.stream()
        .filter(resource
            -> ImmutableSet.of(Kind.Deployment.name(), Kind.StatefulSet.name(), Kind.DaemonSet.name(), Kind.Job.name())
                   .contains(resource.getResourceId().getKind()))
        .filter(resource -> !resource.isDirectApply())
        .collect(Collectors.toList());
  }

  public static KubernetesResource getFirstLoadBalancerService(List<KubernetesResource> resources) {
    List<KubernetesResource> loadBalancerServices =
        resources.stream().filter(resource -> resource.isLoadBalancerService()).collect(Collectors.toList());

    if (loadBalancerServices.size() > 0) {
      return loadBalancerServices.get(0);
    }

    return null;
  }

  public static KubernetesResource getManagedWorkload(List<KubernetesResource> resources) {
    List<KubernetesResource> result = getWorkloads(resources);
    if (!result.isEmpty()) {
      return result.get(0);
    }
    return null;
  }

  public static List<KubernetesResource> getServices(List<KubernetesResource> resources) {
    return resources.stream().filter(resource -> resource.isService()).collect(Collectors.toList());
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
      return null;
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
      return null;
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
      if (o == null || o instanceof Map) {
        // noop
        return true;
      } else {
        throw new KubernetesValuesException("Object is not a map.");
      }
    } catch (YamlException e) {
      String message = e.getMessage();
      if (e.getCause() != null) {
        Throwable cause = e.getCause();
        if (cause instanceof ParserException || cause instanceof TokenizerException) {
          String lineSnippet = getYamlLineKey(valuesFileContent, cause.getMessage());
          message += " " + cause.getMessage() + ". @[" + lineSnippet + "]";
        }
      }
      throw new KubernetesValuesException(message, e.getCause());
    }
  }

  public static Map getMapFromValuesFileContent(String valuesFileContent) {
    Map map = null;

    try {
      YamlReader reader = new YamlReader(valuesFileContent);
      Object o = reader.read();
      if (o == null) {
        return map;
      }
      if (o instanceof Map) {
        map = (Map) o;
      } else {
        throw new WingsException("Invalid Yaml. Object is not a map.");
      }
    } catch (YamlException e) {
      String message = e.getMessage();

      if (e.getCause() != null) {
        Throwable cause = e.getCause();
        if (cause instanceof ParserException || cause instanceof TokenizerException) {
          String lineSnippet = getYamlLineKey(valuesFileContent, cause.getMessage());
          message += " " + cause.getMessage() + ". @[" + lineSnippet + "]";
        }
      }

      throw new WingsException(message, e.getCause());
    }

    return map;
  }

  public static Set<String> getValuesExpressionKeysFromMap(Map map, String parentExpression, int recursionDepth) {
    if (recursionDepth == MAX_VALUES_EXPRESSION_RECURSION_DEPTH) {
      throw new WingsException(
          format("Map is too deep. Max levels supported are %d", MAX_VALUES_EXPRESSION_RECURSION_DEPTH),
          WingsException.USER);
    }

    Set<String> result = new HashSet<>();

    for (Object o : map.entrySet()) {
      Entry<String, Object> entry = (Entry<String, Object>) o;
      String expression = parentExpression + "." + entry.getKey();
      Object value = entry.getValue();

      result.add(VALUES_EXPRESSION + expression);
      if (value instanceof Map) {
        result.addAll(getValuesExpressionKeysFromMap((Map) value, expression, recursionDepth + 1));
      }
    }

    return result;
  }

  private static String getYamlLineKey(String valuesFileContent, String errorMessage) {
    try {
      int lineNumber = Integer.parseInt(
          errorMessage.substring(errorMessage.indexOf("Line ") + "Line ".length(), errorMessage.indexOf(',')));
      String line = valuesFileContent.split(newLineRegex)[lineNumber];
      return line.substring(0, line.indexOf(':'));
    } catch (RuntimeException e) {
      return "<>";
    }
  }

  public static String normalizeFolderPath(String folderPath) {
    if (isBlank(folderPath)) {
      return folderPath;
    }

    return folderPath.endsWith("/") ? folderPath : folderPath + "/";
  }
}
