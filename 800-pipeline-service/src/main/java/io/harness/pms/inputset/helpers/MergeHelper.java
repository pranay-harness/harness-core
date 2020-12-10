package io.harness.pms.inputset.helpers;

import static java.util.stream.Collectors.toMap;

import io.harness.common.NGExpressionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.PipelineYamlConfig;
import io.harness.pms.inputset.fqn.FQN;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetErrorDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetErrorResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetErrorWrapperDTOPMS;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;
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

  public InputSetErrorWrapperDTOPMS getErrorMap(String pipelineYaml, String inputSetYaml) throws IOException {
    String pipelineComp = getPipelineComp(inputSetYaml);
    String templateYaml = createTemplateFromPipeline(pipelineYaml);
    Set<FQN> invalidFQNs = getInvalidFQNsInInputSet(templateYaml, pipelineComp);
    if (EmptyPredicate.isEmpty(invalidFQNs)) {
      return null;
    }

    String errorPipelineYaml = getErrorPipelineYaml(invalidFQNs, pipelineYaml);
    Map<String, InputSetErrorResponseDTOPMS> uuidToErrorResponseMap =
        getUuidToErrorResponseMap(invalidFQNs, getInputSetIdentifier(inputSetYaml));
    return InputSetErrorWrapperDTOPMS.builder()
        .errorPipelineYaml(errorPipelineYaml)
        .uuidToErrorResponseMap(uuidToErrorResponseMap)
        .build();
  }

  private String getErrorPipelineYaml(Set<FQN> invalidFQNs, String pipelineYaml) throws IOException {
    Map<FQN, Object> map = new LinkedHashMap<>();
    invalidFQNs.forEach(fqn -> map.put(fqn, fqn.display()));
    PipelineYamlConfig config = new PipelineYamlConfig(pipelineYaml);
    PipelineYamlConfig res = new PipelineYamlConfig(map, config.getYamlMap());
    return res.getYaml();
  }

  private Map<String, InputSetErrorResponseDTOPMS> getUuidToErrorResponseMap(
      Set<FQN> invalidFQNs, String inputSetIdentifier) {
    Map<String, InputSetErrorResponseDTOPMS> res = new LinkedHashMap<>();
    invalidFQNs.forEach(fqn -> {
      String uuid = fqn.display();
      InputSetErrorDTOPMS errorDTOPMS = InputSetErrorDTOPMS.builder()
                                            .fieldName(fqn.getFieldName())
                                            .message("Field either not present in pipeline or not a runtime input")
                                            .identifierOfErrorSource(inputSetIdentifier)
                                            .build();
      InputSetErrorResponseDTOPMS errorResponseDTOPMS =
          InputSetErrorResponseDTOPMS.builder().errors(Collections.singletonList(errorDTOPMS)).build();
      res.put(uuid, errorResponseDTOPMS);
    });
    return res;
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

  public String mergeInputSetIntoPipeline(String pipelineYaml, String inputSetPipelineCompYaml) throws IOException {
    return mergeInputSetIntoPipeline(pipelineYaml, inputSetPipelineCompYaml, true);
  }

  private String mergeInputSetIntoPipeline(
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

  private String getPipelineComp(String inputSetYaml) {
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
      runtimeInput = MergeHelper.getPipelineComp(runtimeInput);
    }

    String filteredInputSetYaml = MergeHelper.removeRuntimeInputFromYaml(runtimeInput);
    PipelineYamlConfig inputSetConfig = new PipelineYamlConfig(filteredInputSetYaml);

    Set<FQN> invalidFQNsInInputSet = MergeHelper.getInvalidFQNsInInputSet(templateYaml, filteredInputSetYaml);

    Map<FQN, Object> filtered = inputSetConfig.getFqnToValueMap()
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> !invalidFQNsInInputSet.contains(entry.getKey()))
                                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new PipelineYamlConfig(filtered, inputSetConfig.getYamlMap()).getYaml();
  }

  private String getInputSetIdentifier(String inputSetYaml) {
    try {
      JsonNode node = YamlUtils.readTree(inputSetYaml).getNode().getCurrJsonNode();
      ObjectNode innerMap = (ObjectNode) node.get("inputSet");
      JsonNode identifier = innerMap.get("identifier");
      return identifier.asText();
    } catch (IOException e) {
      throw new InvalidRequestException("Input set yaml is invalid");
    }
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
    }
    return res;
  }
}
