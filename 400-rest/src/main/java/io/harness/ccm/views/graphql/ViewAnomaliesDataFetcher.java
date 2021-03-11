package io.harness.ccm.views.graphql;

import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.mappers.QlAnomalyMapper;
import io.harness.ccm.views.service.impl.ViewAnomalyServiceImpl;

import software.wings.graphql.datafetcher.AbstractAnomalyDataFetcher;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class ViewAnomaliesDataFetcher extends AbstractAnomalyDataFetcher<QLCEViewFilterWrapper, QLCEViewGroupBy> {
  @Inject ViewAnomalyServiceImpl anomalyService;

  @Override
  protected QLAnomalyDataList fetch(
      String accountId, List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy) {
    QLAnomalyDataList.QLAnomalyDataListBuilder qlAnomaliesList = QLAnomalyDataList.builder();
    List<AnomalyEntity> anomaliesEntityList = anomalyService.list(filters, groupBy);
    qlAnomaliesList.data(anomaliesEntityList.stream().map(QlAnomalyMapper::toDto).collect(Collectors.toList()));
    return qlAnomaliesList.build();
  }
}
