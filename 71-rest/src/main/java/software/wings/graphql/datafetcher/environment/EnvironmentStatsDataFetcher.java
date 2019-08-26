package software.wings.graphql.datafetcher.environment;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagAggregation;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLEnvironmentFilter,
    QLEnvironmentAggregation, QLTimeSeriesAggregation, QLTagAggregation, QLNoOpSortCriteria> {
  @Inject EnvironmentQueryHelper environmentQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLEnvironmentFilter> filters,
      List<QLEnvironmentAggregation> groupBy, QLTimeSeriesAggregation groupByTime,
      List<QLTagAggregation> tagAggregationList, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Environment.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getAggregationFieldName(String aggregation) {
    QLEnvironmentAggregation qlEnvironmentAggregation = QLEnvironmentAggregation.valueOf(aggregation);
    switch (qlEnvironmentAggregation) {
      case Application:
        return "appId";
      case EnvironmentType:
        return "environmentType";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected void populateFilters(List<QLEnvironmentFilter> filters, Query query) {
    environmentQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.environment;
  }
}
