package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class InputSetYamlHelper {
  public String getPipelineComponent(String inputSetYaml) {
    try {
      if (EmptyPredicate.isEmpty(inputSetYaml)) {
        return inputSetYaml;
      }
      JsonNode node = YamlUtils.readTree(inputSetYaml).getNode().getCurrJsonNode();
      ObjectNode innerMap = (ObjectNode) node.get("inputSet");
      if (innerMap == null) {
        log.warn("Yaml provided is not an input set yaml. Yaml:\n" + inputSetYaml);
        return inputSetYaml;
      }
      JsonNode pipelineNode = innerMap.get("pipeline");
      innerMap.removeAll();
      innerMap.putObject("pipeline");
      innerMap.set("pipeline", pipelineNode);
      return YamlUtils.write(innerMap).replace("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Input set yaml is invalid");
    }
  }

  public String getStringField(String yaml, String fieldName, String topKey) {
    JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
    JsonNode innerMap = node.get(topKey);
    JsonNode field = innerMap.get(fieldName);
    if (field == null) {
      return null;
    }
    return innerMap.get(fieldName).asText().equals("") ? null : innerMap.get(fieldName).asText();
  }

  public boolean isPipelineAbsent(String yaml) {
    JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
    JsonNode innerMap = node.get("inputSet");
    JsonNode field = innerMap.get("pipeline");
    return field == null || field.toString().equals("{}");
  }

  public Map<String, String> getTags(String yaml, String topKey) {
    JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
    JsonNode innerMap = node.get(topKey);
    ObjectNode tags = (ObjectNode) innerMap.get("tags");
    if (tags == null) {
      return null;
    }
    Map<String, String> res = new LinkedHashMap<>();

    Set<String> fieldNames = new LinkedHashSet<>();
    tags.fieldNames().forEachRemaining(fieldNames::add);
    for (String key : fieldNames) {
      String value = tags.get(key).asText();
      res.put(key, value);
    }
    return res;
  }

  public String getTopKey(String yaml) {
    JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
    JsonNode innerMap = node.get("inputSet");
    if (innerMap == null) {
      return "overlayInputSet";
    }
    return "inputSet";
  }

  public List<String> getReferencesFromOverlayInputSetYaml(String yaml) {
    JsonNode node = (new PipelineYamlConfig(yaml)).getYamlMap();
    JsonNode innerMap = node.get("overlayInputSet");
    ArrayNode list = (ArrayNode) innerMap.get("inputSetReferences");
    List<String> res = new ArrayList<>();
    list.forEach(element -> res.add(element.asText()));
    return res;
  }

  public String getInputSetIdentifier(String inputSetYaml) {
    try {
      JsonNode node = YamlUtils.readTree(inputSetYaml).getNode().getCurrJsonNode();
      ObjectNode innerMap = (ObjectNode) node.get("inputSet");
      if (innerMap == null) {
        return "<runtime input yaml>";
      }
      JsonNode identifier = innerMap.get("identifier");
      return identifier.asText();
    } catch (IOException e) {
      throw new InvalidRequestException("Input set yaml is invalid");
    }
  }
}
