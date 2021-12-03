package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.PrometheusDataCollectionInfo;
import io.harness.cvng.beans.PrometheusDataCollectionInfo.MetricCollectionInfo;
import io.harness.cvng.core.entities.PrometheusCVConfig;
import io.harness.cvng.core.entities.PrometheusCVConfig.MetricInfo;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;

public class PrometheusDataCollectionInfoMapper
    implements DataCollectionInfoMapper<PrometheusDataCollectionInfo, PrometheusCVConfig>,
               DataCollectionSLIInfoMapper<PrometheusDataCollectionInfo, PrometheusCVConfig> {
  @Override
  public PrometheusDataCollectionInfo toDataCollectionInfo(PrometheusCVConfig cvConfig) {
    Preconditions.checkNotNull(cvConfig);
    List<MetricCollectionInfo> metricCollectionInfoList = new ArrayList<>();
    cvConfig.getMetricInfoList().forEach(metricInfo -> {
      metricCollectionInfoList.add(getMetricCollectionInfo(metricInfo, metricInfo.getMetricName()));
    });
    return getDataCollectionInfo(metricCollectionInfoList, cvConfig);
  }

  @Override
  public PrometheusDataCollectionInfo toDataCollectionInfo(
      List<PrometheusCVConfig> cvConfigList, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricIdentifiers = serviceLevelIndicator.getMetricIdentifiers();
    Preconditions.checkNotNull(cvConfigList);
    PrometheusCVConfig baseCvConfig = cvConfigList.get(0);
    List<PrometheusDataCollectionInfo.MetricCollectionInfo> metricCollectionInfoList = new ArrayList<>();
    cvConfigList.forEach(cvConfig -> cvConfig.getMetricInfoList().forEach(metricInfo -> {
      if (sliMetricIdentifiers.contains(metricInfo.getIdentifier())) {
        metricCollectionInfoList.add(getMetricCollectionInfo(metricInfo, metricInfo.getIdentifier()));
      }
    }));

    return getDataCollectionInfo(metricCollectionInfoList, baseCvConfig);
  }

  private MetricCollectionInfo getMetricCollectionInfo(MetricInfo metricInfo, String metricName) {
    // using identifier for sli and metricName for live monitoring to keep the behavior same
    // moving to identifier requires larger refactoring
    return PrometheusDataCollectionInfo.MetricCollectionInfo.builder()
        .metricName(metricName)
        .query(metricInfo.getQuery())
        .filters(metricInfo.getFilters())
        .serviceInstanceField(metricInfo.getServiceInstanceFieldName())
        .build();
  }

  private PrometheusDataCollectionInfo getDataCollectionInfo(
      List<MetricCollectionInfo> metricDefinitions, PrometheusCVConfig cvConfig) {
    PrometheusDataCollectionInfo dataCollectionInfo = PrometheusDataCollectionInfo.builder()
                                                          .groupName(cvConfig.getGroupName())
                                                          .metricCollectionInfoList(metricDefinitions)
                                                          .build();
    dataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return dataCollectionInfo;
  }
}
