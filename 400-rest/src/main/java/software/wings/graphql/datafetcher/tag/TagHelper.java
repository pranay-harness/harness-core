package software.wings.graphql.datafetcher.tag;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.graphql.schema.type.aggregation.QLEntityType;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rktummala on 09/08/2019
 */
@Slf4j
@Singleton
@TargetModule(Module._380_CG_GRAPHQL)
public class TagHelper {
  private static final String INVALID_ENTITY_ERROR = "Invalid entityId: %s for entityType: %s";
  private static final String INVALID_ENTITY_TYPE = "Invalid entity type: %s";

  @Inject protected HarnessTagService tagService;
  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected HPersistence persistence;

  // Returns set of all unique entity ids that match the tags for given entity type
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

  public String validateAndFetchAppId(String entityId, QLEntityType entityType) {
    String appId = "";

    switch (entityType) {
      case APPLICATION:
        Application application = persistence.get(Application.class, entityId);
        if (application != null) {
          appId = application.getUuid();
        }
        break;
      case SERVICE:
        Service service = persistence.get(Service.class, entityId);
        if (service != null) {
          appId = service.getAppId();
        }
        break;
      case ENVIRONMENT:
        Environment environment = persistence.get(Environment.class, entityId);
        if (environment != null) {
          appId = environment.getAppId();
        }
        break;
      case WORKFLOW:
        Workflow workflow = persistence.get(Workflow.class, entityId);
        if (workflow != null) {
          appId = workflow.getAppId();
        }
        break;
      case PIPELINE:
        Pipeline pipeline = persistence.get(Pipeline.class, entityId);
        if (pipeline != null) {
          appId = pipeline.getAppId();
        }
        break;
      default:
        throw new InvalidRequestException(format(INVALID_ENTITY_TYPE, entityType.name()));
    }

    if (EmptyPredicate.isEmpty(appId)) {
      throw new InvalidRequestException(format(INVALID_ENTITY_ERROR, entityId, entityType.name()));
    }

    return appId;
  }
}
