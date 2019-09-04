package software.wings.graphql.datafetcher.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowEntityAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilter;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorkflowStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLWorkflowFilter,
    QLWorkflowAggregation, QLNoOpSortCriteria> {
  @Inject WorkflowQueryHelper workflowQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLWorkflowFilter> filters,
      List<QLWorkflowAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Workflow.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream()
                        .filter(g -> g != null && g.getEntityAggregation() != null)
                        .map(g -> g.getEntityAggregation().name())
                        .collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getAggregationFieldName(String aggregation) {
    QLWorkflowEntityAggregation workflowAggregation = QLWorkflowEntityAggregation.valueOf(aggregation);
    switch (workflowAggregation) {
      case Application:
        return "appId";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected void populateFilters(String accountId, List<QLWorkflowFilter> filters, Query query) {
    workflowQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.workflow;
  }
}
