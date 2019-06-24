package software.wings.graphql.datafetcher.application;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationFilter;

import java.util.List;

public class ApplicationStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLApplicationFilter,
    QLApplicationAggregation, QLTimeSeriesAggregation> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLApplicationFilter> filters,
      List<QLApplicationAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    Query<Application> query = wingsPersistence.createQuery(Application.class);
    query.filter("accountId", accountId);

    return getSingleDataPointData(query);
  }
}
