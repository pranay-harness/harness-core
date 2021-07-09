package io.harness.pms.merger.helpers;

import static io.harness.pms.yaml.validation.InputSetValidatorType.ALLOWED_VALUES;
import static io.harness.pms.yaml.validation.InputSetValidatorType.REGEX;

import static java.util.stream.Collectors.toMap;

import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.pms.yaml.validation.InputSetValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
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
          || (!keepInput && !NGExpressionUtils.matchesInputSetPattern(value) && !key.isIdentifierOrVariableName()
              && !key.isType())) {
        templateMap.put(key, fullMap.get(key));
      }
    });
    return (new PipelineYamlConfig(templateMap, pipeline.getYamlMap())).getYaml();
  }

  public Map<FQN, String> getInvalidFQNsInInputSet(String templateYaml, String inputSetPipelineCompYaml)
      throws IOException {
    Map<FQN, String> errorMap = new LinkedHashMap<>();
    PipelineYamlConfig inputSetConfig = new PipelineYamlConfig(inputSetPipelineCompYaml);
    Set<FQN> inputSetFQNs = new LinkedHashSet<>(inputSetConfig.getFqnToValueMap().keySet());
    if (EmptyPredicate.isEmpty(templateYaml)) {
      inputSetFQNs.forEach(fqn -> errorMap.put(fqn, "Pipeline no longer contains any runtime input"));
      return errorMap;
    }
    PipelineYamlConfig templateConfig = new PipelineYamlConfig(templateYaml);

    templateConfig.getFqnToValueMap().keySet().forEach(key -> {
      if (inputSetFQNs.contains(key)) {
        String error = validateStaticValues(
            templateConfig.getFqnToValueMap().get(key), inputSetConfig.getFqnToValueMap().get(key));
        if (EmptyPredicate.isNotEmpty(error)) {
          errorMap.put(key, error);
        }
        inputSetFQNs.remove(key);
      } else {
        Map<FQN, Object> subMap =
            io.harness.pms.merger.helpers.FQNUtils.getSubMap(inputSetConfig.getFqnToValueMap(), key);
        subMap.keySet().forEach(inputSetFQNs::remove);
      }
    });
    inputSetFQNs.forEach(fqn -> errorMap.put(fqn, "Field either not present in pipeline or not a runtime input"));
    return errorMap;
  }

  private String validateStaticValues(Object templateObject, Object inputSetObject) {
    String error = "";
    String templateValue = ((JsonNode) templateObject).asText();
    String inputSetValue = ((JsonNode) inputSetObject).asText();

    if (NGExpressionUtils.matchesInputSetPattern(templateValue)
        && !NGExpressionUtils.isRuntimeOrExpressionField(inputSetValue)) {
      try {
        ParameterField<?> templateField = YamlUtils.read(templateValue, ParameterField.class);
        if (templateField.getInputSetValidator() == null) {
          return error;
        }
        InputSetValidator inputSetValidator = templateField.getInputSetValidator();
        if (inputSetValidator.getValidatorType() == REGEX) {
          boolean matchesPattern =
              NGExpressionUtils.matchesPattern(Pattern.compile(inputSetValidator.getParameters()), inputSetValue);
          error = matchesPattern ? "" : "The value provided does not match the required regex pattern";
        } else if (inputSetValidator.getValidatorType() == ALLOWED_VALUES) {
          String[] allowedValues = inputSetValidator.getParameters().split(", *");
          boolean matches = false;
          for (String allowedValue : allowedValues) {
            if (NGExpressionUtils.isRuntimeOrExpressionField(allowedValue)) {
              return error;
            } else if (allowedValue.equals(inputSetValue)) {
              matches = true;
            }
          }
          error = matches ? "" : "The value provided does not match any of the allowed values";
        }
      } catch (IOException e) {
        throw new InvalidRequestException(
            "Input set expression " + templateValue + " or " + inputSetValue + " is not valid");
      }
    }
    return error;
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
        Map<FQN, Object> subMap =
            io.harness.pms.merger.helpers.FQNUtils.getSubMap(inputSetConfig.getFqnToValueMap(), key);
        if (!subMap.isEmpty()) {
          res.put(key, FQNUtils.getObject(inputSetConfig, key));
        }
      }
    });
    return (new PipelineYamlConfig(res, pipelineConfig.getYamlMap())).getYaml();
  }

  private static void throwUpdatedKeyException(FQN key, Object templateValue, Object value) {
    throw new InvalidRequestException("The value for " + key.getExpressionFqn() + " is " + templateValue.toString()
        + "in the pipeline yaml, but the input set has it as " + value.toString());
  }

  public String getPipelineComponent(String inputSetYaml) {
    try {
      if (EmptyPredicate.isEmpty(inputSetYaml)) {
        return inputSetYaml;
      }
      JsonNode node = YamlUtils.readTree(inputSetYaml).getNode().getCurrJsonNode();
      ObjectNode innerMap = (ObjectNode) node.get("inputSet");
      if (innerMap == null) {
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

  public String sanitizeInputSet(String pipelineYaml, String inputSetYaml) throws IOException {
    return sanitizeInputSet(pipelineYaml, inputSetYaml, true);
  }

  public String sanitizeRuntimeInput(String pipelineYaml, String runtimeInputYaml) throws IOException {
    return sanitizeInputSet(pipelineYaml, runtimeInputYaml, false);
  }

  private String sanitizeInputSet(String pipelineYaml, String runtimeInput, boolean isInputSet) throws IOException {
    String templateYaml = MergeHelper.createTemplateFromPipeline(pipelineYaml);

    if (templateYaml == null) {
      return "";
    }

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

    Set<FQN> invalidFQNsInInputSet = getInvalidFQNsInInputSet(templateYaml, filteredInputSetYaml).keySet();

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
      return ParameterField.createExpressionField(true, ((JsonNode) inputSetValue).asText(),
          parameterField.getInputSetValidator(), ((JsonNode) inputSetValue).getNodeType() != JsonNodeType.STRING);
    } catch (IOException e) {
      log.error("", e);
      return inputSetValue;
    }
  }
}
