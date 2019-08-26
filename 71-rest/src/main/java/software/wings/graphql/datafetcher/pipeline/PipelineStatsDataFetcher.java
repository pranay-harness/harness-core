package software.wings.graphql.datafetcher.pipeline;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineAggregation;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PipelineStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLPipelineFilter,
    QLPipelineAggregation, QLTimeSeriesAggregation, QLTagAggregation, QLNoOpSortCriteria> {
  @Inject PipelineQueryHelper pipelineQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLPipelineFilter> filters,
      List<QLPipelineAggregation> groupBy, QLTimeSeriesAggregation groupByTime,
      List<QLTagAggregation> tagAggregationList, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Pipeline.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
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
  protected void populateFilters(List<QLPipelineFilter> filters, Query query) {
    pipelineQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.pipeline;
  }
}
