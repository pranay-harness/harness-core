package io.harness.pms.inputset.helpers;

import static io.harness.pms.merger.helpers.MergeHelper.createTemplateFromPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.getInvalidFQNsInInputSet;
import static io.harness.pms.merger.helpers.MergeHelper.getPipelineComponent;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.InputSetErrorDTOPMS;
import io.harness.pms.inputset.InputSetErrorResponseDTOPMS;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.merger.PipelineYamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MergeHelper {
  public InputSetErrorWrapperDTOPMS getErrorMap(String pipelineYaml, String inputSetYaml) throws IOException {
    String pipelineComp = getPipelineComponent(inputSetYaml);
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
