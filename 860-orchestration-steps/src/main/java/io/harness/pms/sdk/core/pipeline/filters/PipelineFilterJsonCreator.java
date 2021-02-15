package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineFilterJsonCreator extends ChildrenFilterJsonCreator<PipelineInfoConfig> {
  @Override
  public Class<PipelineInfoConfig> getFieldClass() {
    return PipelineInfoConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("pipeline", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  @Override
  public Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext) {
    YamlField stagesYamlNode = filterCreationContext.getCurrentField().getNode().getField("stages");
    if (stagesYamlNode == null) {
      throw new InvalidRequestException("Pipeline without stages cannot be saved");
    }
    return StagesFilterJsonCreator.getDependencies(stagesYamlNode);
  }

  public List<String> getStageNames(FilterCreationContext filterCreationContext, Collection<YamlField> children) {
    List<String> stageNames = new ArrayList<>();
    for (YamlField stage : children) {
      if (stage.getName().equals(YAMLFieldNameConstants.PARALLEL)) {
        stageNames.addAll(Optional.of(stage.getNode().asArray())
                              .orElse(Collections.emptyList())
                              .stream()
                              .map(el -> el.getField(YAMLFieldNameConstants.STAGE))
                              .filter(Objects::nonNull)
                              .map(YamlField::getNode)
                              .map(YamlNode::getName)
                              .collect(Collectors.toList()));
      } else if (stage.getName().equals(YAMLFieldNameConstants.STAGE)
          && EmptyPredicate.isNotEmpty(stage.getNode().getName())) {
        stageNames.add(stage.getNode().getName());
      }
    }
    return stageNames;
  }

  @Override
  public PipelineFilter getFilterForGivenField() {
    return null;
  }

  @Override
  int getStageCount(FilterCreationContext filterCreationContext, Collection<YamlField> children) {
    return StagesFilterJsonCreator.getStagesCount(children);
  }
}
