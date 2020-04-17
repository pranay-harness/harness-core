package software.wings.graphql.datafetcher.tag;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 09/08/2019
 */
@Slf4j
@Singleton
public class TagHelper {
  @Inject protected HarnessTagService tagService;
  @Inject protected WorkflowExecutionService workflowExecutionService;

  public Set<String> getEntityIdsFromTags(String accountId, List<QLTagInput> tags, EntityType entityType) {
    if (isNotEmpty(tags)) {
      Set<String> entityIds = new HashSet<>();
      tags.forEach(tag -> {
        Set<String> entityIdsForTag =
            tagService.getEntityIdsWithTag(accountId, tag.getName(), entityType, tag.getValue());
        if (isNotEmpty(entityIdsForTag)) {
          entityIds.addAll(entityIdsForTag);
        }
      });
      return entityIds;
    }
    return null;
  }

  public Set<String> getWorkExecutionsWithTags(String accountId, List<QLTagInput> tags) {
    if (isNotEmpty(tags)) {
      Set<String> entityIds = new HashSet<>();
      tags.forEach(tag -> {
        Set<String> workflowExecutionsWithTags =
            workflowExecutionService.getWorkflowExecutionsWithTag(accountId, tag.getName(), tag.getValue());
        if (isNotEmpty(workflowExecutionsWithTags)) {
          entityIds.addAll(workflowExecutionsWithTags);
        }
      });
      return entityIds;
    }
    return null;
  }
}
