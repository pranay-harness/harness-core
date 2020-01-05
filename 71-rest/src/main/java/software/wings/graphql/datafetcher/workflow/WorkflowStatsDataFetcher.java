package software.wings.graphql.datafetcher.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcherWithTags;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowEntityAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilter;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowTagAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowTagType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WorkflowStatsDataFetcher
    extends RealTimeStatsDataFetcherWithTags<QLNoOpAggregateFunction, QLWorkflowFilter, QLWorkflowAggregation,
        QLNoOpSortCriteria, QLWorkflowTagType, QLWorkflowTagAggregation, QLWorkflowEntityAggregation> {
  @Inject WorkflowQueryHelper workflowQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLWorkflowFilter> filters,
      List<QLWorkflowAggregation> groupByList, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Workflow.class;
    final List<String> groupByEntityList = new ArrayList<>();
    if (isNotEmpty(groupByList)) {
      groupByList.forEach(groupBy -> {
        if (groupBy.getEntityAggregation() != null) {
          groupByEntityList.add(groupBy.getEntityAggregation().name());
        }

        if (groupBy.getTagAggregation() != null) {
          QLWorkflowEntityAggregation groupByEntityFromTag = getGroupByEntityFromTag(groupBy.getTagAggregation());
          if (groupByEntityFromTag != null) {
            groupByEntityList.add(groupByEntityFromTag.name());
          }
        }
      });
    }
    return getQLData(accountId, filters, entityClass, groupByEntityList);
  }

  @Override
  public String getAggregationFieldName(String aggregation) {
    QLWorkflowEntityAggregation workflowAggregation = QLWorkflowEntityAggregation.valueOf(aggregation);
    switch (workflowAggregation) {
      case Application:
        return "appId";
      default:
        throw new InvalidRequestException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public void populateFilters(String accountId, List<QLWorkflowFilter> filters, Query query) {
    workflowQueryHelper.setQuery(filters, query, accountId);
  }

  @Override
  public String getEntityType() {
    return NameService.workflow;
  }

  @Override
  protected QLWorkflowTagAggregation getTagAggregation(QLWorkflowAggregation groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected EntityType getEntityType(QLWorkflowTagType entityType) {
    return workflowQueryHelper.getEntityType(entityType);
  }

  @Override
  protected QLWorkflowEntityAggregation getEntityAggregation(QLWorkflowAggregation groupBy) {
    return groupBy.getEntityAggregation();
  }

  @Override
  protected QLWorkflowEntityAggregation getGroupByEntityFromTag(QLWorkflowTagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLWorkflowEntityAggregation.Application;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }
}
