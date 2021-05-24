package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.PrometheusDataCollectionInfo;
import io.harness.cvng.beans.PrometheusDataCollectionInfo.MetricCollectionInfo;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class PrometheusDataCollectionInfoMapper
    implements DataCollectionInfoMapper<PrometheusDataCollectionInfo, PrometheusCVConfig> {
  @Override
  public PrometheusDataCollectionInfo toDataCollectionInfo(PrometheusCVConfig cvConfig) {
    Preconditions.checkNotNull(cvConfig);
    List<MetricCollectionInfo> metricCollectionInfoList = new ArrayList<>();
    cvConfig.getMetricInfoList().forEach(metricInfo -> {
      metricCollectionInfoList.add(MetricCollectionInfo.builder()
                                       .metricName(metricInfo.getMetricName())
                                       .query(metricInfo.getQuery())
                                       .filters(metricInfo.getFilters())
                                       .build());
    });
    PrometheusDataCollectionInfo dataCollectionInfo = PrometheusDataCollectionInfo.builder()
                                                          .groupName(cvConfig.getGroupName())
                                                          .metricCollectionInfoList(metricCollectionInfoList)
                                                          .build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
