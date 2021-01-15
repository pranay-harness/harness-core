package io.harness.pms.merger.helpers;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.fqn.FQNNode;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FQNUtils {
  public Map<FQN, Object> getSubMap(Map<FQN, Object> fullMap, FQN baseFQN) {
    Map<FQN, Object> res = new LinkedHashMap<>();
    fullMap.keySet().forEach(key -> {
      if (key.contains(baseFQN)) {
        res.put(key, fullMap.get(key));
      }
    });
    return res;
  }

  public Map<FQN, Object> generateFQNMap(JsonNode yamlMap) {
    Set<String> fieldNames = new LinkedHashSet<>();
    yamlMap.fieldNames().forEachRemaining(fieldNames::add);
    String topKey = fieldNames.iterator().next();

    FQNNode startNode = FQNNode.builder().nodeType(FQNNode.NodeType.KEY).key(topKey).build();
    FQN currentFQN = FQN.builder().fqnList(Collections.singletonList(startNode)).build();

    Map<FQN, Object> res = new LinkedHashMap<>();

    generateFQNMap(yamlMap.get(topKey), currentFQN, res);
    return res;
  }

  private void generateFQNMap(JsonNode map, FQN baseFQN, Map<FQN, Object> res) {
    Set<String> fieldNames = new LinkedHashSet<>();
    map.fieldNames().forEachRemaining(fieldNames::add);
    for (String key : fieldNames) {
      JsonNode value = map.get(key);
      FQN currFQN = FQN.duplicateAndAddNode(baseFQN, FQNNode.builder().nodeType(FQNNode.NodeType.KEY).key(key).build());
      if (value.getNodeType() == JsonNodeType.ARRAY) {
        ArrayNode arrayNode = (ArrayNode) value;
        generateFQNMapFromList(arrayNode, currFQN, res);
      } else if (value.getNodeType() == JsonNodeType.OBJECT) {
        generateFQNMap(value, currFQN, res);
      } else {
        res.put(currFQN, value);
      }
    }
  }

  private void generateFQNMapFromList(ArrayNode list, FQN baseFQN, Map<FQN, Object> res) {
    if (list == null || list.get(0) == null) {
      return;
    }
    JsonNode firstNode = list.get(0);
    if (firstNode.getNodeType() != JsonNodeType.OBJECT) {
      res.put(baseFQN, list);
      return;
    }

    int noOfKeys = firstNode.size();
    if (noOfKeys == 1) {
      generateFQNMapFromListOfSingleKeyMaps(list, baseFQN, res);
    } else {
      generateFQNMapFromListOfMultipleKeyMaps(list, baseFQN, res);
    }
  }

  private void generateFQNMapFromListOfSingleKeyMaps(ArrayNode list, FQN baseFQN, Map<FQN, Object> res) {
    if (checkIfListHasNoIdentifier(list)) {
      res.put(baseFQN, list);
      return;
    }
    list.forEach(element -> {
      if (element.has(YAMLFieldNameConstants.PARALLEL)) {
        FQN currFQN = FQN.duplicateAndAddNode(baseFQN, FQNNode.builder().nodeType(FQNNode.NodeType.PARALLEL).build());
        ArrayNode listOfMaps = (ArrayNode) element.get(YAMLFieldNameConstants.PARALLEL);
        generateFQNMapFromList(listOfMaps, currFQN, res);
      } else {
        Set<String> fieldNames = new LinkedHashSet<>();
        element.fieldNames().forEachRemaining(fieldNames::add);
        String topKey = fieldNames.iterator().next();
        JsonNode innerMap = element.get(topKey);
        String identifier = innerMap.get(YAMLFieldNameConstants.IDENTIFIER).asText();
        FQN currFQN = FQN.duplicateAndAddNode(baseFQN,
            FQNNode.builder()
                .nodeType(FQNNode.NodeType.KEY_WITH_UUID)
                .key(topKey)
                .uuidKey(YAMLFieldNameConstants.IDENTIFIER)
                .uuidValue(identifier)
                .build());
        generateFQNMap(innerMap, currFQN, res);
      }
    });
  }

  private boolean checkIfListHasNoIdentifier(ArrayNode list) {
    JsonNode firstNode = list.get(0);
    Set<String> fieldNames = new LinkedHashSet<>();
    firstNode.fieldNames().forEachRemaining(fieldNames::add);
    String topKey = fieldNames.iterator().next();
    if (topKey.equals(YAMLFieldNameConstants.PARALLEL)) {
      return false;
    }
    JsonNode innerMap = firstNode.get(topKey);
    return !innerMap.has(YAMLFieldNameConstants.IDENTIFIER);
  }

  private void generateFQNMapFromListOfMultipleKeyMaps(ArrayNode list, FQN baseFQN, Map<FQN, Object> res) {
    String uuidKey = getUuidKey(list);
    if (EmptyPredicate.isEmpty(uuidKey)) {
      res.put(baseFQN, list);
      return;
    }
    list.forEach(element -> {
      FQN currFQN = FQN.duplicateAndAddNode(baseFQN,
          FQNNode.builder()
              .nodeType(FQNNode.NodeType.UUID)
              .uuidKey(uuidKey)
              .uuidValue(element.get(uuidKey).asText())
              .build());
      if (uuidKey.equals(YAMLFieldNameConstants.IDENTIFIER)) {
        generateFQNMap(element, currFQN, res);
      } else {
        Set<String> fieldNames = new LinkedHashSet<>();
        element.fieldNames().forEachRemaining(fieldNames::add);
        for (String key : fieldNames) {
          FQN finalFQN =
              FQN.duplicateAndAddNode(currFQN, FQNNode.builder().nodeType(FQNNode.NodeType.KEY).key(key).build());
          res.put(finalFQN, element.get(key));
        }
      }
    });
  }

  public JsonNode generateYamlMap(Map<FQN, Object> fqnMap, JsonNode originalYaml) throws IOException {
    Set<String> fieldNames = new LinkedHashSet<>();
    originalYaml.fieldNames().forEachRemaining(fieldNames::add);
    String topKey = fieldNames.iterator().next();

    FQNNode startNode = FQNNode.builder().nodeType(FQNNode.NodeType.KEY).key(topKey).build();
    FQN currentFQN = FQN.builder().fqnList(Collections.singletonList(startNode)).build();

    Map<String, Object> tempMap = new LinkedHashMap<>();
    generateYamlMap(getSubMap(fqnMap, currentFQN), currentFQN, originalYaml.get(topKey), tempMap, topKey);
    return YamlUtils.readTree(YamlUtils.write(tempMap).replace("---\n", "")).getNode().getCurrJsonNode();
  }

  private void generateYamlMap(
      Map<FQN, Object> fqnMap, FQN baseFQN, JsonNode originalYaml, Map<String, Object> res, String topKey) {
    Set<String> fieldNames = new LinkedHashSet<>();
    originalYaml.fieldNames().forEachRemaining(fieldNames::add);
    Map<String, Object> tempMap = new LinkedHashMap<>();
    for (String key : fieldNames) {
      JsonNode value = originalYaml.get(key);
      FQN currFQN = FQN.duplicateAndAddNode(baseFQN, FQNNode.builder().nodeType(FQNNode.NodeType.KEY).key(key).build());
      if (fqnMap.containsKey(currFQN)) {
        tempMap.put(key, fqnMap.get(currFQN));
      } else if (value.getNodeType() == JsonNodeType.ARRAY) {
        ArrayNode arrayNode = (ArrayNode) value;
        generateYamlMapFromList(arrayNode, currFQN, fqnMap, tempMap, key);
      } else if (value.getNodeType() == JsonNodeType.OBJECT) {
        generateYamlMap(getSubMap(fqnMap, currFQN), currFQN, value, tempMap, key);
      }
    }
    if (!tempMap.isEmpty()) {
      if (!fieldNames.contains(YAMLFieldNameConstants.IDENTIFIER)) {
        res.put(topKey, tempMap);
      } else {
        Map<String, Object> newTempMap = new LinkedHashMap<>();
        newTempMap.put(YAMLFieldNameConstants.IDENTIFIER, originalYaml.get(YAMLFieldNameConstants.IDENTIFIER));
        newTempMap.putAll(tempMap);
        res.put(topKey, newTempMap);
      }
    }
  }

  private void generateYamlMapFromList(
      ArrayNode list, FQN baseFQN, Map<FQN, Object> fqnMap, Map<String, Object> res, String topKey) {
    if (list == null || list.get(0) == null) {
      return;
    }
    JsonNode firstNode = list.get(0);
    if (firstNode.getNodeType() != JsonNodeType.OBJECT) {
      if (fqnMap.containsKey(baseFQN)) {
        res.put(topKey, list);
      }
      return;
    }
    int noOfKeys = firstNode.size();
    if (noOfKeys == 1) {
      generateYamlMapFromListOfSingleKeyMaps(list, baseFQN, fqnMap, res, topKey);
    } else {
      generateYamlMapFromListOfMultipleKeyMaps(list, baseFQN, fqnMap, res, topKey);
    }
  }

  private void generateYamlMapFromListOfSingleKeyMaps(
      ArrayNode list, FQN baseFQN, Map<FQN, Object> fqnMap, Map<String, Object> res, String topKey) {
    List<Object> topKeyList = new ArrayList<>();
    if (checkIfListHasNoIdentifier(list)) {
      if (fqnMap.containsKey(baseFQN)) {
        topKeyList.add(list);
        res.put(topKey, topKeyList);
      }
      return;
    }
    list.forEach(element -> {
      if (element.has(YAMLFieldNameConstants.PARALLEL)) {
        FQN currFQN = FQN.duplicateAndAddNode(baseFQN, FQNNode.builder().nodeType(FQNNode.NodeType.PARALLEL).build());
        ArrayNode listOfMaps = (ArrayNode) element.get(YAMLFieldNameConstants.PARALLEL);
        Map<String, Object> tempMap = new LinkedHashMap<>();
        generateYamlMapFromList(
            listOfMaps, currFQN, getSubMap(fqnMap, currFQN), tempMap, YAMLFieldNameConstants.PARALLEL);
        if (!tempMap.isEmpty()) {
          topKeyList.add(tempMap);
        }
      } else {
        Set<String> fieldNames = new LinkedHashSet<>();
        element.fieldNames().forEachRemaining(fieldNames::add);
        String topKeyOfInnerMap = fieldNames.iterator().next();
        JsonNode innerMap = element.get(topKeyOfInnerMap);
        String identifier = innerMap.get(YAMLFieldNameConstants.IDENTIFIER).asText();
        FQN currFQN = FQN.duplicateAndAddNode(baseFQN,
            FQNNode.builder()
                .nodeType(FQNNode.NodeType.KEY_WITH_UUID)
                .key(topKeyOfInnerMap)
                .uuidKey(YAMLFieldNameConstants.IDENTIFIER)
                .uuidValue(identifier)
                .build());
        Map<String, Object> tempMap = new LinkedHashMap<>();
        generateYamlMap(getSubMap(fqnMap, currFQN), currFQN, innerMap, tempMap, topKeyOfInnerMap);
        if (!tempMap.isEmpty()) {
          topKeyList.add(tempMap);
        }
      }
    });
    if (!topKeyList.isEmpty()) {
      res.put(topKey, topKeyList);
    }
  }

  private void generateYamlMapFromListOfMultipleKeyMaps(
      ArrayNode list, FQN baseFQN, Map<FQN, Object> fqnMap, Map<String, Object> res, String topKey) {
    List<Object> topKeyList = new ArrayList<>();
    String uuidKey = getUuidKey(list);
    if (EmptyPredicate.isEmpty(uuidKey)) {
      if (fqnMap.containsKey(baseFQN)) {
        topKeyList.add(list);
        res.put(topKey, topKeyList);
      }
      return;
    }
    list.forEach(element -> {
      FQN currFQN = FQN.duplicateAndAddNode(baseFQN,
          FQNNode.builder()
              .nodeType(FQNNode.NodeType.UUID)
              .uuidKey(uuidKey)
              .uuidValue(element.get(uuidKey).asText())
              .build());
      Map<String, Object> tempRes = new LinkedHashMap<>();
      if (uuidKey.equals(YAMLFieldNameConstants.IDENTIFIER)) {
        generateYamlMap(fqnMap, currFQN, element, tempRes, topKey);
        if (tempRes.containsKey(topKey)) {
          topKeyList.add(tempRes.get(topKey));
        }
      } else {
        Map<String, Object> tempMap = new LinkedHashMap<>();
        Set<String> fieldNames = new LinkedHashSet<>();
        element.fieldNames().forEachRemaining(fieldNames::add);
        for (String key : fieldNames) {
          FQN finalFQN =
              FQN.duplicateAndAddNode(currFQN, FQNNode.builder().nodeType(FQNNode.NodeType.KEY).key(key).build());
          if (fqnMap.containsKey(finalFQN)) {
            tempMap.put(key, fqnMap.get(finalFQN));
          }
        }
        if (!tempMap.isEmpty()) {
          Map<String, Object> newTempMap = new LinkedHashMap<>();
          newTempMap.put(uuidKey, element.get(uuidKey));
          newTempMap.putAll(tempMap);
          topKeyList.add(newTempMap);
        }
      }
    });
    if (!topKeyList.isEmpty()) {
      res.put(topKey, topKeyList);
    }
  }

  private String getUuidKey(ArrayNode list) {
    JsonNode element = list.get(0);
    if (element.has(YAMLFieldNameConstants.IDENTIFIER)) {
      return YAMLFieldNameConstants.IDENTIFIER;
    }
    if (element.has(YAMLFieldNameConstants.NAME)) {
      return YAMLFieldNameConstants.NAME;
    }
    if (element.has(YAMLFieldNameConstants.KEY)) {
      return YAMLFieldNameConstants.KEY;
    }
    return "";
  }

  public JsonNode getObject(PipelineYamlConfig config, FQN baseFQN) {
    JsonNode curr = config.getYamlMap();
    boolean prevWasParallel = false;
    for (FQNNode node : baseFQN.getFqnList()) {
      if (prevWasParallel) {
        curr = getObjectFromParallel((ArrayNode) curr, node);
        prevWasParallel = false;
      } else if (node.getNodeType() == FQNNode.NodeType.KEY) {
        curr = curr.get(node.getKey());
      } else if (node.getNodeType() == FQNNode.NodeType.KEY_WITH_UUID) {
        curr = getObjectFromArrayNode((ArrayNode) curr, node);
      } else if (node.getNodeType() == FQNNode.NodeType.PARALLEL) {
        prevWasParallel = true;
      }
    }
    return curr;
  }

  private JsonNode getObjectFromArrayNode(ArrayNode curr, FQNNode node) {
    int size = curr.size();
    for (int i = 0; i < size; i++) {
      JsonNode elem = curr.get(i).get(node.getKey());
      String identifier = elem.get(node.getUuidKey()).asText();
      if (identifier.equals(node.getUuidValue())) {
        return elem;
      }
    }
    throw new InvalidRequestException("Could not find node in the list");
  }

  private JsonNode getObjectFromParallel(ArrayNode curr, FQNNode node) {
    int size = curr.size();
    for (int i = 0; i < size; i++) {
      JsonNode elem = curr.get(i);
      if (!elem.has(YAMLFieldNameConstants.PARALLEL)) {
        continue;
      }
      ArrayNode innerList = (ArrayNode) elem.get(YAMLFieldNameConstants.PARALLEL);
      int sizeOfInnerList = innerList.size();
      for (int j = 0; j < sizeOfInnerList; j++) {
        JsonNode element = innerList.get(j).get(node.getKey());
        String identifier = element.get(node.getUuidKey()).asText();
        if (identifier.equals(node.getUuidValue())) {
          return element;
        }
      }
    }
    throw new InvalidRequestException("Could not find node in the list");
  }
}
