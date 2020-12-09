package io.harness.pms.inputset.helpers;

import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.PipelineYamlConfig;
import io.harness.pms.inputset.fqn.FQN;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MergeHelper {
  public String createTemplateFromPipeline(String pipelineYaml, boolean keepInput) throws IOException {
    PipelineYamlConfig pipeline = new PipelineYamlConfig(pipelineYaml);
    Map<FQN, Object> fullMap = pipeline.getFqnToValueMap();
    Map<FQN, Object> templateMap = new LinkedHashMap<>();
    fullMap.keySet().forEach(key -> {
      String value = fullMap.get(key).toString().replace("\"", "");
      if ((keepInput && NGExpressionUtils.matchesInputSetPattern(value))
          || (!keepInput && !NGExpressionUtils.matchesInputSetPattern(value) && !key.isIdentifierOrVariableName())) {
        templateMap.put(key, fullMap.get(key));
      }
    });
    return (new PipelineYamlConfig(templateMap, pipeline.getYamlMap())).getYaml();
  }

  public Set<FQN> getInvalidFQNsInInputSet(String templateYaml, String inputSetPipelineCompYaml) throws IOException {
    PipelineYamlConfig inputSetConfig = new PipelineYamlConfig(inputSetPipelineCompYaml);
    PipelineYamlConfig templateConfig = new PipelineYamlConfig(templateYaml);
    Set<FQN> res = new LinkedHashSet<>(inputSetConfig.getFqnToValueMap().keySet());
    templateConfig.getFqnToValueMap().keySet().forEach(key -> {
      if (res.contains(key)) {
        res.remove(key);
      } else {
        Map<FQN, Object> subMap = FQNUtils.getSubMap(inputSetConfig.getFqnToValueMap(), key);
        subMap.keySet().forEach(res::remove);
      }
    });
    return res;
  }

  public String mergeInputSetIntoPipeline(
      String pipelineYaml, String inputSetPipelineCompYaml, boolean convertToTemplate) throws IOException {
    PipelineYamlConfig pipelineConfig = new PipelineYamlConfig(pipelineYaml);
    String templateYaml = createTemplateFromPipeline(pipelineYaml, true);
    if (!convertToTemplate) {
      templateYaml = pipelineYaml;
    }
    PipelineYamlConfig inputSetConfig = new PipelineYamlConfig(inputSetPipelineCompYaml);
    PipelineYamlConfig templateConfig = new PipelineYamlConfig(templateYaml);

    Map<FQN, Object> res = new LinkedHashMap<>(pipelineConfig.getFqnToValueMap());
    templateConfig.getFqnToValueMap().keySet().forEach(key -> {
      if (inputSetConfig.getFqnToValueMap().containsKey(key)) {
        res.put(key, inputSetConfig.getFqnToValueMap().get(key));
      } else {
        Map<FQN, Object> subMap = FQNUtils.getSubMap(inputSetConfig.getFqnToValueMap(), key);
        if (!subMap.isEmpty()) {
          res.put(key, FQNUtils.getObject(inputSetConfig, key));
        }
      }
    });
    return (new PipelineYamlConfig(res, pipelineConfig.getYamlMap())).getYaml();
  }

  public String mergeInputSets(String template, List<String> inputSetYamlList) throws IOException {
    List<String> inputSetPipelineCompYamlList =
        inputSetYamlList.stream().map(MergeHelper::getPipelineComp).collect(Collectors.toList());
    String res = template;
    for (String yaml : inputSetPipelineCompYamlList) {
      res = mergeInputSetIntoPipeline(res, yaml, false);
    }
    return createTemplateFromPipeline(res, false);
  }

  public String getPipelineComp(String inputSetYaml) {
    try {
      JsonNode node = YamlUtils.readTree(inputSetYaml).getNode().getCurrJsonNode();
      ObjectNode innerMap = (ObjectNode) node.get("inputSet");
      JsonNode pipelineNode = innerMap.get("pipeline");
      innerMap.removeAll();
      innerMap.putObject("pipeline");
      innerMap.set("pipeline", pipelineNode);
      return YamlUtils.write(innerMap).replace("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Input set yaml is invalid");
    }
  }
}
