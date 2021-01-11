package io.harness.jackson;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonNodeUtils {
  public static JsonNode deletePropertiesInJsonNode(ObjectNode jsonNode, String... properties) {
    if (isEmpty(properties) || jsonNode == null) {
      return jsonNode;
    }
    for (String property : properties) {
      if (jsonNode.has(property)) {
        jsonNode.remove(property);
      }
    }
    return jsonNode;
  }

  public static JsonNode updatePropertiesInJsonNode(ObjectNode jsonNode, Map<String, String> properties) {
    if (isEmpty(properties) || jsonNode == null) {
      return jsonNode;
    }
    for (Map.Entry<String, String> property : properties.entrySet()) {
      if (jsonNode.has(property.getKey())) {
        jsonNode.put(property.getKey(), property.getValue());
      }
    }
    return jsonNode;
  }

  public static JsonNode setPropertiesInJsonNodeWithArrayKey(ObjectNode jsonNode, String key, String... values) {
    if (isEmpty(key) || jsonNode == null) {
      return jsonNode;
    }
    if (jsonNode.has(key)) {
      final JsonNode keyNode = jsonNode.get(key);
      if (keyNode.isArray()) {
        final ArrayNode keyNodeArray = (ArrayNode) keyNode;
        keyNodeArray.removeAll();
        for (String value : values) {
          keyNodeArray.add(value);
        }
      }
    } else {
      ObjectMapper objectMapper = new ObjectMapper();
      final ArrayNode arrayNode = objectMapper.createArrayNode();
      List<String> valuesList = new ArrayList<>(Arrays.asList(values));
      valuesList.forEach(arrayNode::add);
      jsonNode.putArray(key).addAll(arrayNode);
    }

    return jsonNode;
  }

  // In case key is arraynode values are added to array.
  public static JsonNode upsertPropertyInObjectNode(JsonNode objectNode, String key, String... values) {
    if (isEmpty(key) || objectNode == null) {
      return objectNode;
    }

    List<String> valuesList = new ArrayList<>(Arrays.asList(values));
    if (isEmpty(valuesList)) {
      return objectNode;
    }
    final JsonNode keyNode = objectNode.get(key);
    if (keyNode != null) {
      if (keyNode.isArray()) {
        final ArrayNode keyNodeArray = (ArrayNode) keyNode;

        for (String value : valuesList) {
          keyNodeArray.add(value);
        }
      } else if (keyNode.isObject()) {
        // We can't add multiple values in an object node. Hence updating whole keynode.
        addValuesToObjectNode((ObjectNode) objectNode, key, valuesList);
      }
    } else {
      addValuesToObjectNode((ObjectNode) objectNode, key, valuesList);
    }

    return objectNode;
  }

  private static void addValuesToObjectNode(ObjectNode objectNode, String key, List<String> valuesList) {
    if (valuesList.size() == 1) {
      objectNode.put(key, valuesList.get(0));
    } else {
      ObjectMapper objectMapper = new ObjectMapper();
      final ArrayNode arrayNode = objectMapper.createArrayNode();
      valuesList.forEach(arrayNode::add);
      objectNode.putArray(key).addAll(arrayNode);
    }
  }
}
