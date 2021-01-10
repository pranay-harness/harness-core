package io.harness.pms.merger.helpers;

import static java.util.stream.Collectors.toMap;

import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MergeHelper {
  public String createTemplateFromPipeline(String pipelineYaml) throws IOException {
    return createTemplateFromPipeline(pipelineYaml, true);
  }

  public String removeRuntimeInputFromYaml(String runtimeInputYaml) throws IOException {
    return createTemplateFromPipeline(runtimeInputYaml, false);
  }

  private String createTemplateFromPipeline(String pipelineYaml, boolean keepInput) throws IOException {
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
        Map<FQN, Object> subMap =
            io.harness.pms.merger.helpers.FQNUtils.getSubMap(inputSetConfig.getFqnToValueMap(), key);
        subMap.keySet().forEach(res::remove);
      }
    });
    return res;
  }

  public String mergeInputSetIntoPipeline(
      String pipelineYaml, String inputSetPipelineCompYaml, boolean appendInputSetValidator) throws IOException {
    return mergeInputSetIntoPipeline(pipelineYaml, inputSetPipelineCompYaml, true, appendInputSetValidator);
  }

  private String mergeInputSetIntoPipeline(String pipelineYaml, String inputSetPipelineCompYaml,
      boolean convertToTemplate, boolean appendInputSetValidator) throws IOException {
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
        Object value = inputSetConfig.getFqnToValueMap().get(key);
        if (appendInputSetValidator) {
          value = checkForRuntimeInputExpressions(value, templateConfig.getFqnToValueMap().get(key));
        }
        res.put(key, value);
      } else {
        Map<FQN, Object> subMap =
            io.harness.pms.merger.helpers.FQNUtils.getSubMap(inputSetConfig.getFqnToValueMap(), key);
        if (!subMap.isEmpty()) {
          res.put(key, FQNUtils.getObject(inputSetConfig, key));
        }
      }
    });
    return (new PipelineYamlConfig(res, pipelineConfig.getYamlMap())).getYaml();
  }

  public String getPipelineComponent(String inputSetYaml) {
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

  public String sanitizeInputSet(String pipelineYaml, String inputSetYaml) throws IOException {
    return sanitizeInputSet(pipelineYaml, inputSetYaml, true);
  }

  public String sanitizeRuntimeInput(String pipelineYaml, String runtimeInputYaml) throws IOException {
    return sanitizeInputSet(pipelineYaml, runtimeInputYaml, false);
  }

  private String sanitizeInputSet(String pipelineYaml, String runtimeInput, boolean isInputSet) throws IOException {
    String templateYaml = MergeHelper.createTemplateFromPipeline(pipelineYaml);

    // Strip off inputSet top key from yaml.
    // when its false, its runtimeInput (may be coming from trigger)
    if (isInputSet) {
      runtimeInput = getPipelineComponent(runtimeInput);
    }

    String filteredInputSetYaml = MergeHelper.removeRuntimeInputFromYaml(runtimeInput);
    if (EmptyPredicate.isEmpty(filteredInputSetYaml)) {
      return "";
    }
    PipelineYamlConfig inputSetConfig = new PipelineYamlConfig(filteredInputSetYaml);

    Set<FQN> invalidFQNsInInputSet = getInvalidFQNsInInputSet(templateYaml, filteredInputSetYaml);

    Map<FQN, Object> filtered = inputSetConfig.getFqnToValueMap()
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> !invalidFQNsInInputSet.contains(entry.getKey()))
                                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new PipelineYamlConfig(filtered, inputSetConfig.getYamlMap()).getYaml();
  }

  public String mergeInputSets(String template, List<String> inputSetYamlList, boolean appendInputSetValidator)
      throws IOException {
    List<String> inputSetPipelineCompYamlList =
        inputSetYamlList.stream().map(MergeHelper::getPipelineComponent).collect(Collectors.toList());
    String res = template;
    for (String yaml : inputSetPipelineCompYamlList) {
      res = mergeInputSetIntoPipeline(res, yaml, false, appendInputSetValidator);
    }
    return createTemplateFromPipeline(res, false);
  }

  private Object checkForRuntimeInputExpressions(Object inputSetValue, Object pipelineValue) {
    String pipelineValText = ((JsonNode) pipelineValue).asText();
    if (!NGExpressionUtils.matchesInputSetPattern(pipelineValText)) {
      return inputSetValue;
    }
    try {
      ParameterField<?> parameterField = YamlUtils.read(pipelineValText, ParameterField.class);
      if (parameterField.getInputSetValidator() == null) {
        return inputSetValue;
      }
      return ParameterField.createExpressionField(true, ((JsonNode) inputSetValue).asText(),
          parameterField.getInputSetValidator(), ((JsonNode) inputSetValue).getNodeType() != JsonNodeType.STRING);
    } catch (IOException e) {
      e.printStackTrace();
      return inputSetValue;
    }
  }
}
