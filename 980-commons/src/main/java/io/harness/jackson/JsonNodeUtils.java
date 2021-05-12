package io.harness.jackson;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class JsonNodeUtils {
  public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
    if (mainNode.isObject() && updateNode.isObject()) {
      mergeInternal(mainNode, updateNode);
    } else if (mainNode.isArray() && updateNode.isArray()) {
      mergeJsonArray((ArrayNode) mainNode, (ArrayNode) updateNode);
    }
    return mainNode;
  }

  private static JsonNode mergeInternal(JsonNode mainNode, JsonNode updateNode) {
    Iterator<String> fieldNames = updateNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode jsonNode = mainNode.get(fieldName);
      if (jsonNode != null && jsonNode.isObject()) {
        merge(jsonNode, updateNode.get(fieldName));
      } else {
        if (mainNode instanceof ObjectNode) {
          JsonNode value = updateNode.get(fieldName);
          ((ObjectNode) mainNode).set(fieldName, value);
        }
      }
    }
    return mainNode;
  }

  private static void mergeJsonArray(ArrayNode src, ArrayNode other) {
    for (int i = 0; i < other.size(); i++) {
      JsonNode s = src.get(i);
      JsonNode v = other.get(i);
      if (s == null) {
        src.add(v);
      } else if (v.isObject() && s.isObject()) {
        merge(s, v);
      } else if (v.isArray() && s.isArray()) {
        mergeJsonArray((ArrayNode) s, (ArrayNode) v);
      } else {
        src.remove(i);
        src.insert(i, v);
      }
    }
  }

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

  public Boolean mustGetBoolean(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, BooleanNode.class);
    return getOrThrow(fieldName, isNull(field) ? null : field.booleanValue());
  }

  public Boolean getBoolean(JsonNode node, String fieldName) {
    return getBoolean(node, fieldName, null);
  }

  public Boolean getBoolean(JsonNode node, String fieldName, Boolean defaultValue) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, BooleanNode.class);
    return getOrDefault(isNull(field) ? null : field.booleanValue(), defaultValue);
  }

  public String mustGetString(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, TextNode.class);
    return getOrThrow(fieldName, isNull(field) ? null : field.textValue());
  }

  public String getString(JsonNode node, String fieldName) {
    return getString(node, fieldName, null);
  }

  public String getString(JsonNode node, String fieldName, String defaultValue) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, TextNode.class);
    return getOrDefault(isNull(field) ? null : field.textValue(), defaultValue);
  }

  public Long mustGetLong(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, NumericNode.class);
    return getOrThrow(fieldName, isNull(field) ? null : field.longValue());
  }

  public Integer getLong(JsonNode node, String fieldName) {
    return getLong(node, fieldName, null);
  }

  public Integer getLong(JsonNode node, String fieldName, Integer defaultValue) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, NumericNode.class);
    return getOrDefault(isNull(field) ? null : field.intValue(), defaultValue);
  }

  public Map<String, JsonNode> getMap(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    checkType(fieldName, field, ObjectNode.class);
    if (isNull(field)) {
      return Collections.emptyMap();
    }

    Map<String, JsonNode> map = new HashMap<>();
    field.fields().forEachRemaining(f -> {
      if (EmptyPredicate.isNotEmpty(f.getKey()) && !isNull(f.getValue())) {
        map.put(f.getKey(), f.getValue());
      }
    });
    return map;
  }

  public boolean isNull(JsonNode node) {
    return node == null || node.isNull();
  }

  private void checkType(String fieldName, JsonNode node, Class<?> cls) {
    if (!isNull(node) && !cls.isAssignableFrom(node.getClass())) {
      throw new InvalidArgumentsException(String.format("Field [%s] of invalid type: expected %s, got %s", fieldName,
          cls.getSimpleName(), node.getClass().getSimpleName()));
    }
  }

  private <T> T getOrDefault(T value, T defaultValue) {
    return value == null ? defaultValue : value;
  }

  private <T> T getOrThrow(String fieldName, T value) {
    if (value == null) {
      throw new InvalidArgumentsException(String.format("Field not found: %s", fieldName));
    }
    return value;
  }
}
