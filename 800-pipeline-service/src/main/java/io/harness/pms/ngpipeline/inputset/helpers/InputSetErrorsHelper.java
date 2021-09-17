package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.beans.InputSetValidatorType.ALLOWED_VALUES;
import static io.harness.beans.InputSetValidatorType.REGEX;
import static io.harness.pms.merger.helpers.InputSetYamlHelper.getPipelineComponent;
import static io.harness.pms.merger.helpers.YamlTemplateHelper.createTemplateFromPipeline;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.InputSetErrorDTOPMS;
import io.harness.pms.inputset.InputSetErrorResponseDTOPMS;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.InputSetYamlHelper;
import io.harness.pms.merger.helpers.YamlSubMapExtractor;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.pms.yaml.validation.InputSetValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class InputSetErrorsHelper {
  public InputSetErrorWrapperDTOPMS getErrorMap(String pipelineYaml, String inputSetYaml) {
    String pipelineComp = getPipelineComponent(inputSetYaml);
    String templateYaml = createTemplateFromPipeline(pipelineYaml);
    Map<FQN, String> invalidFQNs = getInvalidFQNsInInputSet(templateYaml, pipelineComp);
    if (EmptyPredicate.isEmpty(invalidFQNs)) {
      return null;
    }

    String errorPipelineYaml = getErrorPipelineYaml(invalidFQNs.keySet(), pipelineYaml);
    Map<String, InputSetErrorResponseDTOPMS> uuidToErrorResponseMap = getUuidToErrorResponseMap(
        invalidFQNs, InputSetYamlHelper.getStringField(inputSetYaml, "identifier", "inputSet"));
    return InputSetErrorWrapperDTOPMS.builder()
        .errorPipelineYaml(errorPipelineYaml)
        .uuidToErrorResponseMap(uuidToErrorResponseMap)
        .build();
  }

  private String getErrorPipelineYaml(Set<FQN> invalidFQNs, String pipelineYaml) {
    Map<FQN, Object> map = new LinkedHashMap<>();
    invalidFQNs.forEach(fqn -> map.put(fqn, fqn.getExpressionFqn()));
    YamlConfig config = new YamlConfig(pipelineYaml);
    YamlConfig res = new YamlConfig(map, config.getYamlMap());
    return res.getYaml();
  }

  public Map<String, InputSetErrorResponseDTOPMS> getUuidToErrorResponseMap(
      String pipelineYaml, String inputSetPipelineComponent) {
    String templateYaml = createTemplateFromPipeline(pipelineYaml);
    Map<FQN, String> invalidFQNs = getInvalidFQNsInInputSet(templateYaml, inputSetPipelineComponent);
    if (EmptyPredicate.isEmpty(invalidFQNs)) {
      return null;
    }
    return getUuidToErrorResponseMap(invalidFQNs, "");
  }

  private Map<String, InputSetErrorResponseDTOPMS> getUuidToErrorResponseMap(
      Map<FQN, String> invalidFQNs, String inputSetIdentifier) {
    Map<String, InputSetErrorResponseDTOPMS> res = new LinkedHashMap<>();
    invalidFQNs.keySet().forEach(fqn -> {
      String uuid = fqn.getExpressionFqn();
      InputSetErrorDTOPMS errorDTOPMS = InputSetErrorDTOPMS.builder()
                                            .fieldName(fqn.getFieldName())
                                            .message(invalidFQNs.get(fqn))
                                            .identifierOfErrorSource(inputSetIdentifier)
                                            .build();
      InputSetErrorResponseDTOPMS errorResponseDTOPMS =
          InputSetErrorResponseDTOPMS.builder().errors(Collections.singletonList(errorDTOPMS)).build();
      res.put(uuid, errorResponseDTOPMS);
    });
    return res;
  }

  public Map<String, String> getInvalidInputSetReferences(
      List<Optional<InputSetEntity>> inputSets, List<String> identifiers) {
    Map<String, String> res = new LinkedHashMap<>();
    for (int i = 0; i < identifiers.size(); i++) {
      String identifier = identifiers.get(i);
      Optional<InputSetEntity> entity = inputSets.get(i);
      if (!entity.isPresent()) {
        res.put(identifier, "Reference does not exist");
        continue;
      }
      if (entity.get().getInputSetEntityType() == InputSetEntityType.OVERLAY_INPUT_SET) {
        res.put(identifier, "References can't be other overlay input sets");
      }
      if (entity.get().getIsInvalid()) {
        res.put(identifier, "Reference is an invalid input set");
      }
    }
    return res;
  }

  public Map<FQN, String> getInvalidFQNsInInputSet(String templateYaml, String inputSetPipelineCompYaml) {
    Map<FQN, String> errorMap = new LinkedHashMap<>();
    YamlConfig inputSetConfig = new YamlConfig(inputSetPipelineCompYaml);
    Set<FQN> inputSetFQNs = new LinkedHashSet<>(inputSetConfig.getFqnToValueMap().keySet());
    if (EmptyPredicate.isEmpty(templateYaml)) {
      inputSetFQNs.forEach(fqn -> errorMap.put(fqn, "Pipeline no longer contains any runtime input"));
      return errorMap;
    }
    YamlConfig templateConfig = new YamlConfig(templateYaml);

    templateConfig.getFqnToValueMap().keySet().forEach(key -> {
      if (inputSetFQNs.contains(key)) {
        Object templateValue = templateConfig.getFqnToValueMap().get(key);
        Object value = inputSetConfig.getFqnToValueMap().get(key);
        if (key.isType() || key.isIdentifierOrVariableName()) {
          if (!value.toString().equals(templateValue.toString())) {
            errorMap.put(key,
                "The value for " + key.getExpressionFqn() + " is " + templateValue.toString()
                    + "in the pipeline yaml, but the input set has it as " + value.toString());
          }
        } else {
          String error = validateStaticValues(templateValue, value);
          if (EmptyPredicate.isNotEmpty(error)) {
            errorMap.put(key, error);
          }
        }

        inputSetFQNs.remove(key);
      } else {
        Map<FQN, Object> subMap = YamlSubMapExtractor.getFQNToObjectSubMap(inputSetConfig.getFqnToValueMap(), key);
        subMap.keySet().forEach(inputSetFQNs::remove);
      }
    });
    inputSetFQNs.forEach(fqn -> errorMap.put(fqn, "Field either not present in pipeline or not a runtime input"));
    return errorMap;
  }

  private String validateStaticValues(Object templateObject, Object inputSetObject) {
    /*
      This if block is to validate static values inside an array of primitive values.
      For example, the pipeline can have something like `a: <+input>.regex(a.*a)`, and the input set provides
      the following value `a: [austria, australia, india]`. This block checks each element in the list against the
      regex provided in the pipeline yaml.
     */
    if (inputSetObject instanceof ArrayNode) {
      ArrayNode inputSetValueArray = (ArrayNode) inputSetObject;
      List<JsonNode> invalidJsonNodes = new ArrayList<>();
      for (JsonNode element : inputSetValueArray) {
        String error = validateStaticValues(templateObject, element);
        if (EmptyPredicate.isNotEmpty(error)) {
          invalidJsonNodes.add(element);
        }
      }
      if (invalidJsonNodes.isEmpty()) {
        return "";
      }
      List<String> invalidValues = invalidJsonNodes.stream().map(JsonNode::textValue).collect(Collectors.toList());
      return "Following values don't match the provided input set validator: " + invalidValues;
    }
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
}
