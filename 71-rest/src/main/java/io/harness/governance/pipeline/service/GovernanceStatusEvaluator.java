package io.harness.governance.pipeline.service;

import static java.util.stream.Collectors.toList;

import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.model.Tag;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.entityinterface.TagAware;

import java.util.List;

/**
 * Evaluates how a tagged entity scores in respect to a governance standard.
 * @param <T>
 */
public interface GovernanceStatusEvaluator<T extends TagAware> {
  enum EntityType { PIPELINE, WORKFLOW }
  GovernanceRuleStatus status(String accountId, T entity, PipelineGovernanceRule rule);

  static boolean containsAll(List<HarnessTagLink> links, List<Tag> tagsToLookFor) {
    List<Tag> linksAsTags = links.stream().map(link -> new Tag(link.getKey(), link.getValue())).collect(toList());
    return linksAsTags.containsAll(tagsToLookFor);
  }

  static boolean containsAny(List<HarnessTagLink> links, List<Tag> tagsToLookFor) {
    List<Tag> linksAsTags = links.stream().map(link -> new Tag(link.getKey(), link.getValue())).collect(toList());
    return tagsToLookFor.stream().anyMatch(linksAsTags::contains);
  }
}
