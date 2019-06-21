package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationFilter;

import java.util.List;

public class ApplicationStatsDataFetcher extends AbstractStatsDataFetcher<QLAggregateFunction, QLApplicationFilter,
    QLApplicationAggregation, QLTimeSeriesAggregation> {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLApplicationFilter> filters,
      List<QLApplicationAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    Query<Application> query = wingsPersistence.createQuery(Application.class);
    query.filter("accountId", accountId);

    long count = query.count();
    return QLSinglePointData.builder().dataPoint(QLDataPoint.builder().value(count).build()).build();
  }
}
