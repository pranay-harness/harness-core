package software.wings.graphql.datafetcher;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

import java.util.List;

public abstract class RealTimeStatsDataFetcherWithTags<A, F, G, S, E, TA extends TagAggregation, EA>
    extends AbstractStatsDataFetcherWithTags<A, F, G, S, E, TA, EA> implements BaseRealTimeStatsDataFetcher<F> {
  protected QLData getQLData(String accountId, List<F> filters, Class entityClass, List<String> groupByAsStringList) {
    return getQLData(utils, nameService, wingsPersistence, accountId, filters, entityClass, groupByAsStringList);
  }
}
