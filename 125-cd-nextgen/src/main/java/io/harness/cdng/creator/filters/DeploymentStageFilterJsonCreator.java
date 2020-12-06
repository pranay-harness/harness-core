package io.harness.cdng.creator.filters;

import io.harness.cdng.creator.plan.stage.DeploymentStageConfig;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DeploymentStageFilterJsonCreator implements FilterJsonCreator<DeploymentStageConfig> {
  @Override
  public Class<DeploymentStageConfig> getFieldClass() {
    return DeploymentStageConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("Deployment"));
  }

  @Override
  public FilterCreationResponse handleNode(
      FilterCreationContext filterCreationContext, DeploymentStageConfig yamlField) {
    return null;
  }
}
