package software.wings.graphql.datafetcher.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.service.QLServiceAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceEntityAggregation;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;
import software.wings.graphql.utils.nameservice.NameService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLServiceFilter,
    QLServiceAggregation, QLNoOpSortCriteria> {
  @Inject ServiceQueryHelper serviceQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLServiceFilter> filters,
      List<QLServiceAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Service.class;
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
    QLServiceEntityAggregation serviceAggregation = QLServiceEntityAggregation.valueOf(aggregation);
    switch (serviceAggregation) {
      case Application:
        return "appId";
      case ArtifactType:
        return "artifactType";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected void populateFilters(String accountId, List<QLServiceFilter> filters, Query query) {
    serviceQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.service;
  }
}
