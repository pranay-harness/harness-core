package io.harness.k8s.manifest;

import static io.fabric8.kubernetes.api.KubernetesHelper.createYamlObjectMapper;
import static io.fabric8.kubernetes.api.KubernetesHelper.toYaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourceUtils {
  // this method is a workaround until we default to NON_EMPTY on the kubernetes model
  // see: https://github.com/fabric8io/kubernetes-model/issues/154

  // This method is copied from KubernetesHelper.java
  public static String toYamlNotEmpty(HasMetadata entity) throws IOException {
    String yaml = toYaml(entity);
    ObjectMapper objectMapper = createYamlObjectMapper();

    // TODO we must convert to YAML, parse as JsonNode
    // then remove empty nodes then write to YAML
    // again which is a hack around this issue:
    // https://github.com/fabric8io/kubernetes-model/issues/154
    JsonNode jsonNode = objectMapper.readTree(yaml);
    removeNullOrEmptyValues(jsonNode);

    return objectMapper.writeValueAsString(jsonNode);
  }

  private static void removeNullOrEmptyValues(JsonNode jsonNode) {
    if (jsonNode instanceof ObjectNode) {
      List<String> removeFields = new ArrayList<>();
      ObjectNode object = (ObjectNode) jsonNode;
      for (Iterator<String> iter = object.fieldNames(); iter.hasNext();) {
        String field = iter.next();
        JsonNode value = object.get(field);
        if (isEmptyValue(value)) {
          removeFields.add(field);
        } else {
          removeNullOrEmptyValues(value);
        }
      }
      for (String field : removeFields) {
        object.remove(field);
      }
    } else if (jsonNode instanceof ArrayNode) {
      ArrayNode arrayNode = (ArrayNode) jsonNode;
      for (int i = 0, size = arrayNode.size(); i < size; i++) {
        JsonNode value = arrayNode.get(i);
        removeNullOrEmptyValues(value);
      }
    }
  }

  private static boolean isEmptyValue(JsonNode value) {
    if (value.isArray()) {
      int size = value.size();
      return size == 0;
    }
    if (value.isTextual()) {
      String text = value.textValue();
      return text == null;
    }
    if (value.isObject()) {
      removeNullOrEmptyValues(value);
      Iterator<String> iter = value.fieldNames();
      int count = 0;
      while (iter.hasNext()) {
        iter.next();
        count++;
      }
      return count == 0;
    }
    return false;
  }
}
