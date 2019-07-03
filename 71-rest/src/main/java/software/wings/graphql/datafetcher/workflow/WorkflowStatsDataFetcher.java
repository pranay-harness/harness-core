package software.wings.graphql.datafetcher.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.WingsException;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowAggregation;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilter;
import software.wings.graphql.schema.type.aggregation.workflow.QLWorkflowFilterType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorkflowStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLWorkflowFilter,
    QLWorkflowAggregation, QLTimeSeriesAggregation, QLNoOpSortCriteria> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLWorkflowFilter> filters,
      List<QLWorkflowAggregation> groupBy, QLTimeSeriesAggregation groupByTime, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Workflow.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getFilterFieldName(String filterType) {
    QLWorkflowFilterType qlFilterType = QLWorkflowFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Application:
        return "appId";
      case Workflow:
        return "_id";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  protected String getAggregationFieldName(String aggregation) {
    QLWorkflowAggregation workflowAggregation = QLWorkflowAggregation.valueOf(aggregation);
    switch (workflowAggregation) {
      case Application:
        return "appId";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public String getEntityType() {
    return NameService.workflow;
  }
}
