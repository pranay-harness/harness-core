package software.wings.graphql.datafetcher.environment;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.WingsException;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilterType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLEnvironmentFilter,
    QLEnvironmentAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLEnvironmentFilter> filters,
      List<QLEnvironmentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    final Class entityClass = Environment.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getFilterFieldName(String filterType) {
    QLEnvironmentFilterType qlFilterType = QLEnvironmentFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Application:
        return "appId";
      case Environment:
        return "_id";
      case EnvironmentType:
        return "environmentType";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
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

  protected String getAggregationNameField(String aggregation) {
    QLEnvironmentAggregation qlEnvironmentAggregation = QLEnvironmentAggregation.valueOf(aggregation);
    switch (qlEnvironmentAggregation) {
      case Application:
        return "appName";
      case EnvironmentType:
        return "environmentType";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }
}
