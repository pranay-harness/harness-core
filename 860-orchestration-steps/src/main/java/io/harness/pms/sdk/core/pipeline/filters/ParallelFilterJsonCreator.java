package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ParallelFilterJsonCreator extends ChildrenFilterJsonCreator<YamlField> {
  @Override
  public Map<String, YamlField> getDependencies(FilterCreationContext filterCreationContext) {
    return Optional.of(filterCreationContext.getCurrentField().getNode().asArray())
        .orElse(Collections.emptyList())
        .stream()
        .map(el -> el.getField("stage"))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(field -> field.getNode().getUuid(), field -> field));
  }

  @Override
  public PipelineFilter getFilterForGivenField() {
    return null;
  }

  @Override
  int getStageCount(FilterCreationContext filterCreationContext, Collection<YamlField> children) {
    return children.size();
  }

  @Override
  public Class<YamlField> getFieldClass() {
    return YamlField.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("parallel", Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
