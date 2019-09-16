package io.harness.governance.pipeline.service.evaluators;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.govern.Switch;
import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.service.GovernanceStatusEvaluator;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Pipeline;
import software.wings.features.api.Usage;
import software.wings.service.intfc.HarnessTagService;

import java.util.Collections;
import java.util.List;

@Singleton
public class PipelineStatusEvaluator implements GovernanceStatusEvaluator<Pipeline> {
  @Inject private HarnessTagService harnessTagService;

  @Override
  public GovernanceRuleStatus status(String accountId, Pipeline pipeline, PipelineGovernanceRule rule) {
    List<HarnessTagLink> pipelineTags = harnessTagService.fetchTagsForEntity(accountId, pipeline);

    boolean tagsIncluded = false;

    switch (rule.getMatchType()) {
      case ALL:
        tagsIncluded = GovernanceStatusEvaluator.containsAll(pipelineTags, rule.getTags());
        break;
      case ANY:
        tagsIncluded = GovernanceStatusEvaluator.containsAny(pipelineTags, rule.getTags());
        break;
      default:
        Switch.unhandled(rule.getMatchType());
    }

    if (tagsIncluded) {
      Usage tagsLocation = Usage.builder()
                               .entityId(pipeline.getUuid())
                               .entityType(EntityType.PIPELINE.toString())
                               .entityName(pipeline.getName())
                               .build();

      return new GovernanceRuleStatus(
          rule.getTags(), rule.getWeight(), true, rule.getMatchType(), Collections.singletonList(tagsLocation));
    } else {
      return new GovernanceRuleStatus(
          rule.getTags(), rule.getWeight(), false, rule.getMatchType(), Collections.emptyList());
    }
  }
}
