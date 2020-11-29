package io.harness.pms.plan.creator.filters;

import io.harness.pms.filter.FilterCreationResponse;
import io.harness.pms.yaml.YamlField;

import java.util.*;
import java.util.stream.Collectors;

public class PipelineFilterJsonCreator implements FilterJsonCreator<YamlField> {
  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton("__any__"));
  }

  @Override
  public FilterCreationResponse handleNode(YamlField yamlField) {
    YamlField stages = yamlField.getNode().getField("stages");

    FilterCreationResponse creationResponse = FilterCreationResponse.builder().build();
    if (stages == null) {
      return creationResponse;
    }

    // ToDo Add support for parallel stages
    List<YamlField> stageYamlFields = Optional.of(stages.getNode().asArray())
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(el -> el.getField("stage"))
                                          .filter(Objects::nonNull)
                                          .collect(Collectors.toList());

    Map<String, YamlField> stageYamlFieldMap = new HashMap<>();
    stageYamlFields.forEach(stepField -> stageYamlFieldMap.put(stepField.getNode().getUuid(), stepField));
    if (!stageYamlFieldMap.isEmpty()) {
      creationResponse.addDependencies(stageYamlFieldMap);
    }

    return creationResponse;
  }
}
