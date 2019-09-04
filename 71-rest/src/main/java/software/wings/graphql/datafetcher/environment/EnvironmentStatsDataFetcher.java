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
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentEntityAggregation;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnvironmentStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLEnvironmentFilter,
    QLEnvironmentAggregation, QLNoOpSortCriteria> {
  @Inject EnvironmentQueryHelper environmentQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLEnvironmentFilter> filters,
      List<QLEnvironmentAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Environment.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream()
                        .filter(g -> g != null && g.getEntityAggregation() != null)
                        .map(g -> g.getEntityAggregation().name())
                        .collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  protected String getAggregationFieldName(String aggregation) {
    QLEnvironmentEntityAggregation qlEnvironmentAggregation = QLEnvironmentEntityAggregation.valueOf(aggregation);
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
  protected void populateFilters(String accountId, List<QLEnvironmentFilter> filters, Query query) {
    environmentQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.environment;
  }
}
