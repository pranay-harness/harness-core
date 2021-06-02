package io.harness.plancreator.approval;

import io.harness.filters.GenericStageFilterJsonCreator;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;

import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApprovalStageFilterJsonCreator extends GenericStageFilterJsonCreator {
  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("Approval");
  }

  @Override
  public PipelineFilter getFilter(FilterCreationContext filterCreationContext, StageElementConfig stageElementConfig) {
    return null;
  }
}
