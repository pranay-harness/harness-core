package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class MergeHelper {
  public String mergeInputSetIntoPipeline(
      String pipelineYaml, String inputSetPipelineCompYaml, boolean appendInputSetValidator) {
    return mergeInputSetIntoOriginYaml(pipelineYaml, inputSetPipelineCompYaml, true, appendInputSetValidator);
  }

  private String mergeInputSetIntoOriginYaml(
      String originYaml, String inputSetPipelineCompYaml, boolean convertToTemplate, boolean appendInputSetValidator) {
    YamlConfig originYamlConfig = new YamlConfig(originYaml);
    String templateYaml = YamlTemplateHelper.createTemplateFromPipeline(originYaml);
    if (!convertToTemplate) {
      templateYaml = originYaml;
    }
    YamlConfig inputSetConfig = new YamlConfig(inputSetPipelineCompYaml);
    YamlConfig templateConfig = new YamlConfig(templateYaml);

    Map<FQN, Object> res = new LinkedHashMap<>(originYamlConfig.getFqnToValueMap());
    templateConfig.getFqnToValueMap().keySet().forEach(key -> {
      if (inputSetConfig.getFqnToValueMap().containsKey(key)) {
        Object value = inputSetConfig.getFqnToValueMap().get(key);
        Object templateValue = templateConfig.getFqnToValueMap().get(key);
        if (key.isType() || key.isIdentifierOrVariableName()) {
          if (!value.toString().equals(templateValue.toString())) {
            throwUpdatedKeyException(key, templateValue, value);
          }
        }
        if (appendInputSetValidator) {
          value = checkForRuntimeInputExpressions(value, templateConfig.getFqnToValueMap().get(key));
        }
        res.put(key, value);
      } else {
        Map<FQN, Object> subMap = YamlSubMapExtractor.getFQNToObjectSubMap(inputSetConfig.getFqnToValueMap(), key);
        if (!subMap.isEmpty()) {
          res.put(key, YamlSubMapExtractor.getNodeForFQN(inputSetConfig, key));
        }
      }
    });
    return (new YamlConfig(res, originYamlConfig.getYamlMap())).getYaml();
  }

  private void throwUpdatedKeyException(FQN key, Object templateValue, Object value) {
    throw new InvalidRequestException("The value for " + key.getExpressionFqn() + " is " + templateValue.toString()
        + "in the pipeline yaml, but the input set has it as " + value.toString());
  }

  public String mergeInputSets(String template, List<String> inputSetYamlList, boolean appendInputSetValidator) {
    List<String> inputSetPipelineCompYamlList =
        inputSetYamlList.stream().map(InputSetYamlHelper::getPipelineComponent).collect(Collectors.toList());
    String res = template;
    for (String yaml : inputSetPipelineCompYamlList) {
      res = mergeInputSetIntoOriginYaml(res, yaml, false, appendInputSetValidator);
    }
    return res;
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
      /*
      this if block appends the input set validator on every element of a list of primitive types
       */
      if (inputSetValue instanceof ArrayNode) {
        ArrayNode inputSetArray = (ArrayNode) inputSetValue;
        List<ParameterField<?>> appendedValidator = new ArrayList<>();
        for (JsonNode element : inputSetArray) {
          String elementText = element.asText();
          appendedValidator.add(ParameterField.createExpressionField(
              true, elementText, parameterField.getInputSetValidator(), element.getNodeType() != JsonNodeType.STRING));
        }
        return appendedValidator;
      }
      return ParameterField.createExpressionField(true, ((JsonNode) inputSetValue).asText(),
          parameterField.getInputSetValidator(), ((JsonNode) inputSetValue).getNodeType() != JsonNodeType.STRING);
    } catch (IOException e) {
      log.error("", e);
      return inputSetValue;
    }
  }
}
