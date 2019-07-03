package software.wings.graphql.datafetcher.pipeline;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.WingsException;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilterType;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PipelineStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLPipelineFilter,
    QLPipelineAggregation, QLTimeSeriesAggregation, QLNoOpSortCriteria> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLPipelineFilter> filters,
      List<QLPipelineAggregation> groupBy, QLTimeSeriesAggregation groupByTime, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Pipeline.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getFilterFieldName(String filterType) {
    QLPipelineFilterType qlFilterType = QLPipelineFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Application:
        return "appId";
      case Pipeline:
        return "_id";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  protected String getAggregationFieldName(String aggregation) {
    QLPipelineAggregation pipelineAggregation = QLPipelineAggregation.valueOf(aggregation);
    switch (pipelineAggregation) {
      case Application:
        return "appId";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public String getEntityType() {
    return NameService.pipeline;
  }
}
