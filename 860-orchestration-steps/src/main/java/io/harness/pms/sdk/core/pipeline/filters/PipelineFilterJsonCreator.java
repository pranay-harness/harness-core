package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.YamlField;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

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

  @Override
  public PipelineFilter getFilterForGivenField() {
    return null;
  }

  @Override
  int getStageCount(FilterCreationContext filterCreationContext, Collection<YamlField> children) {
    return StagesFilterJsonCreator.getStagesCount(children);
  }
}
