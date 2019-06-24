package software.wings.graphql.datafetcher.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.WingsException;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilterType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLServiceFilter,
    QLServiceAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLServiceFilter> filters,
      List<QLServiceAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    final Class entityClass = Service.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getFilterFieldName(String filterType) {
    QLServiceFilterType serviceFilterType = QLServiceFilterType.valueOf(filterType);
    switch (serviceFilterType) {
      case Application:
        return "appId";
      case Service:
        return "_id";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  protected String getAggregationFieldName(String aggregation) {
    QLServiceAggregation serviceAggregation = QLServiceAggregation.valueOf(aggregation);
    switch (serviceAggregation) {
      case Application:
        return "appId";
      case ArtifactType:
        return "artifactType";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  protected String getAggregationNameField(String aggregation) {
    QLServiceAggregation serviceAggregation = QLServiceAggregation.valueOf(aggregation);
    switch (serviceAggregation) {
      case Application:
        return "appName";
      case ArtifactType:
        return "artifactType";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }
}
