package io.harness.governance.pipeline.service.evaluators;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.govern.Switch;
import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.service.GovernanceStatusEvaluator;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.beans.Workflow;
import software.wings.features.api.Usage;
import software.wings.service.intfc.HarnessTagService;

import java.util.Collections;
import java.util.List;

public class WorkflowStatusEvaluator implements GovernanceStatusEvaluator<Workflow> {
  @Inject private HarnessTagService harnessTagService;

  @Override
  public GovernanceRuleStatus status(
      final String accountId, final Workflow workflow, final PipelineGovernanceRule rule) {
    List<HarnessTagLink> pipelineTags = fetchWorkflowTags(workflow.getUuid(), accountId);

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
                               .entityId(workflow.getUuid())
                               .entityType(EntityType.WORKFLOW.toString())
                               .entityName(workflow.getName())
                               .build();

      return new GovernanceRuleStatus(
          rule.getTags(), rule.getWeight(), true, rule.getMatchType(), Collections.singletonList(tagsLocation));
    } else {
      return new GovernanceRuleStatus(
          rule.getTags(), rule.getWeight(), false, rule.getMatchType(), Collections.emptyList());
    }
  }

  // fetches all tags related to given workflowId
  private List<HarnessTagLink> fetchWorkflowTags(final String workflowId, final String accountId) {
    PageRequest<HarnessTagLink> request = PageRequestBuilder.aPageRequest()
                                              .addFilter(HarnessTagLinkKeys.entityId, Operator.EQ, workflowId)
                                              .addFilter(HarnessTagLinkKeys.accountId, Operator.EQ, accountId)
                                              .build();

    return harnessTagService.listResourcesWithTag(accountId, request);
  }
}
