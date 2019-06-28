package software.wings.graphql.datafetcher.application;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationFilter;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.List;

public class ApplicationStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLApplicationFilter,
    QLApplicationAggregation, QLTimeSeriesAggregation, QLNoOpSortCriteria> {
  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLApplicationFilter> filters,
      List<QLApplicationAggregation> groupBy, QLTimeSeriesAggregation groupByTime,
      List<QLNoOpSortCriteria> sortCriteria) {
    Query<Application> query = wingsPersistence.createQuery(Application.class);
    query.filter("accountId", accountId);

    return getSingleDataPointData(query);
  }

  @Override
  public String getEntityType() {
    return NameService.application;
  }
}
