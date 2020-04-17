package software.wings.graphql.datafetcher.execution;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagType;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class ExecutionQueryHelper {
  @Inject protected DataFetcherUtils utils;
  @Inject protected TagHelper tagHelper;

  public void setQuery(List<QLExecutionFilter> filters, Query query, String accountId) {
    if (isEmpty(filters)) {
      return;
    }

    final boolean[] pipelineExecutionIdInQuery = {false};

    filters.forEach(filter -> {
      FieldEnd<? extends Query<WorkflowExecution>> field;

      if (filter.getApplication() != null) {
        field = query.field(WorkflowExecutionKeys.appId);
        QLIdFilter idFilter = filter.getApplication();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getCloudProvider() != null) {
        field = query.field(WorkflowExecutionKeys.deployedCloudProviders);
        QLIdFilter idFilter = filter.getCloudProvider();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getDuration() != null) {
        field = query.field(WorkflowExecutionKeys.duration);
        QLNumberFilter numberFilter = filter.getDuration();
        utils.setNumberFilter(field, numberFilter);
      }

      if (filter.getEndTime() != null) {
        field = query.field(WorkflowExecutionKeys.endTs);
        QLTimeFilter timeFilter = filter.getEndTime();
        utils.setTimeFilter(field, timeFilter);
      }

      if (filter.getEnvironment() != null) {
        field = query.field(WorkflowExecutionKeys.deployedEnvironments + ".uuid");
        QLIdFilter idFilter = filter.getEnvironment();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getPipeline() != null) {
        field = query.field(WorkflowExecutionKeys.workflowId);
        QLIdFilter idFilter = filter.getPipeline();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getWorkflow() != null) {
        field = query.field(WorkflowExecutionKeys.workflowId);
        QLIdFilter idFilter = filter.getWorkflow();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getService() != null) {
        field = query.field(WorkflowExecutionKeys.deployedServices);
        QLIdFilter idFilter = filter.getService();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getStartTime() != null) {
        field = query.field(WorkflowExecutionKeys.startTs);
        QLTimeFilter timeFilter = filter.getStartTime();
        utils.setTimeFilter(field, timeFilter);
      }

      if (filter.getStatus() != null) {
        field = query.field(WorkflowExecutionKeys.status);
        QLStringFilter stringFilter = filter.getStatus();
        utils.setStringFilter(field, stringFilter);
      }

      if (filter.getTrigger() != null) {
        field = query.field(WorkflowExecutionKeys.deploymentTriggerId);
        QLIdFilter idFilter = filter.getTrigger();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getTriggeredBy() != null) {
        field = query.field(WorkflowExecutionKeys.triggeredBy);
        QLIdFilter idFilter = filter.getTriggeredBy();
        utils.setIdFilter(field, idFilter);
      }

      if (filter.getCreationTime() != null) {
        field = query.field(WorkflowExecutionKeys.createdAt);
        QLTimeFilter timeFilter = filter.getCreationTime();
        utils.setTimeFilter(field, timeFilter);
      }

      if (filter.getPipelineExecution() != null) {
        field = query.field(WorkflowExecutionKeys.pipelineExecutionId);
        QLIdFilter idFilter = filter.getPipelineExecution();
        utils.setIdFilter(field, idFilter);
        /**
         * If we are querying the memberExecutions, then we need to explicitly mark this boolean so we do not include
         * the does not exist in the query
         */
        pipelineExecutionIdInQuery[0] = true;
      }

      if (filter.getTag() != null) {
        QLDeploymentTagFilter tagFilter = filter.getTag();
        List<QLTagInput> tags = tagFilter.getTags();
        Set<String> entityIds;
        if (QLDeploymentTagType.DEPLOYMENT == tagFilter.getEntityType()) {
          entityIds = tagHelper.getWorkExecutionsWithTags(accountId, tags);
        } else {
          entityIds = tagHelper.getEntityIdsFromTags(accountId, tags, getEntityType(tagFilter.getEntityType()));
        }
        switch (tagFilter.getEntityType()) {
          case APPLICATION:
            query.field(WorkflowExecutionKeys.appId).in(entityIds);
            break;
          case SERVICE:
            query.field(WorkflowExecutionKeys.serviceIds).in(entityIds);
            break;
          case ENVIRONMENT:
            query.field(WorkflowExecutionKeys.envIds).in(entityIds);
            break;
          case DEPLOYMENT:
            query.field(WorkflowExecutionKeys.uuid).in(entityIds);
            break;
          default:
            logger.error("EntityType {} not supported in execution query", tagFilter.getEntityType());
            throw new InvalidRequestException("Error while compiling execution query", WingsException.USER);
        }
      }
    });

    /***
     * This is to ensure that we are getting only top level executions.
     */
    if (!pipelineExecutionIdInQuery[0]) {
      query.field(WorkflowExecutionKeys.pipelineExecutionId).doesNotExist();
    }
  }

  public EntityType getEntityType(QLDeploymentTagType entityType) {
    switch (entityType) {
      case APPLICATION:
        return EntityType.APPLICATION;
      case SERVICE:
        return EntityType.SERVICE;
      case ENVIRONMENT:
        return EntityType.ENVIRONMENT;
      case DEPLOYMENT:
        return EntityType.DEPLOYMENT;
      default:
        logger.error("Unsupported entity type {} for tag ", entityType);
        throw new InvalidRequestException("Unsupported entity type " + entityType);
    }
  }
}
