package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.PrometheusDataCollectionInfo;
import io.harness.cvng.beans.PrometheusDataCollectionInfo.MetricCollectionInfo;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

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
                                       .serviceInstanceField(metricInfo.getServiceInstanceFieldName())
                                       .build());
    });
    PrometheusDataCollectionInfo dataCollectionInfo = PrometheusDataCollectionInfo.builder()
                                                          .groupName(cvConfig.getGroupName())
                                                          .metricCollectionInfoList(metricCollectionInfoList)
                                                          .build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }

  @Override
  public PrometheusDataCollectionInfo toDataCollectionInfoForSLI(
      List<PrometheusCVConfig> cvConfigList, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricNames = serviceLevelIndicator.getMetricNames();
    Preconditions.checkNotNull(cvConfigList);
    PrometheusCVConfig baseCvConfig = cvConfigList.get(0);
    List<MetricCollectionInfo> metricCollectionInfoList = new ArrayList<>();
    cvConfigList.forEach(cvConfig -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
      if (sliMetricNames.contains(metricInfo.getMetricName())) {
        metricCollectionInfoList.add(MetricCollectionInfo.builder()
                                         .metricName(metricInfo.getMetricName())
                                         .query(metricInfo.getQuery())
                                         .filters(metricInfo.getFilters())
                                         .serviceInstanceField(metricInfo.getServiceInstanceFieldName())
                                         .build());
      }
    }));
    PrometheusDataCollectionInfo dataCollectionInfo = PrometheusDataCollectionInfo.builder()
                                                          .groupName(baseCvConfig.getGroupName())
                                                          .metricCollectionInfoList(metricCollectionInfoList)
                                                          .build();
    dataCollectionInfo.setDataCollectionDsl(baseCvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
