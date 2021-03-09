package io.harness.ccm.views.graphql;

import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.mappers.QlAnomalyMapper;
import io.harness.ccm.views.service.impl.ViewAnomalyServiceImpl;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList;

import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.stream.Collectors;

public class ViewAnomaliesDataFetcher extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCEViewAggregation,
    QLCEViewFilterWrapper, QLCEViewGroupBy, QLCEViewSortCriteria> {
  @Inject ViewAnomalyServiceImpl anomalyService;

  @Override
  protected QLData fetch(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort,
      Integer limit, Integer offset) {
    QLAnomalyDataList.QLAnomalyDataListBuilder qlAnomaliesList = QLAnomalyDataList.builder();
    List<AnomalyEntity> anomaliesEntityList = anomalyService.list(filters, groupBy, aggregateFunction);
    qlAnomaliesList.data(anomaliesEntityList.stream().map(QlAnomalyMapper::toDto).collect(Collectors.toList()));
    return qlAnomaliesList.build();
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCEViewGroupBy> groupByList,
      List<QLCEViewAggregation> aggregations, List<QLCEViewSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort,
      Integer limit, Integer offset, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return false;
  }
}
