package software.wings.graphql.datafetcher.application;

import software.wings.beans.Application;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationAggregation;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationFilter;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.List;
import org.mongodb.morphia.query.Query;

public class ApplicationStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLApplicationFilter,
    QLApplicationAggregation, QLNoOpSortCriteria> {
  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLApplicationFilter> filters,
      List<QLApplicationAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Application> query = wingsPersistence.createQuery(Application.class);
    query.filter("accountId", accountId);

    return getSingleDataPointData(query);
  }

  @Override
  public void populateFilters(String accountId, List<QLApplicationFilter> filters, Query query) {
    // do nothing
  }

  @Override
  public String getAggregationFieldName(String aggregation) {
    return null;
  }

  @Override
  public String getEntityType() {
    return NameService.application;
  }
}
