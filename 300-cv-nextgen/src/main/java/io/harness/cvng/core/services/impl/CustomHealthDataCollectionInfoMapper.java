package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.beans.CustomHealthDataCollectionInfo;
import io.harness.cvng.beans.MetricResponseMappingDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.MetricResponseMapping;
import io.harness.cvng.core.entities.CustomHealthCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomHealthDataCollectionInfoMapper
    implements DataCollectionInfoMapper<CustomHealthDataCollectionInfo, CustomHealthCVConfig>,
               DataCollectionSLIInfoMapper<CustomHealthDataCollectionInfo, CustomHealthCVConfig> {
  @Override
  public CustomHealthDataCollectionInfo toDataCollectionInfo(CustomHealthCVConfig cvConfig, TaskType taskType) {
    CustomHealthDataCollectionInfo customHealthDataCollectionInfo =
        CustomHealthDataCollectionInfo.builder()
            .groupName(cvConfig.getGroupName())
            .metricInfoList(cvConfig.getMetricDefinitions()
                                .stream()
                                .map(metricDefinition -> mapMetricDefinitionToMetricInfo(metricDefinition))
                                .collect(Collectors.toList()))
            .build();
    customHealthDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return customHealthDataCollectionInfo;
  }

  @Override
  public CustomHealthDataCollectionInfo toDataCollectionInfo(
      List<CustomHealthCVConfig> cvConfigs, ServiceLevelIndicator serviceLevelIndicator) {
    if (isEmpty(cvConfigs) || serviceLevelIndicator == null) {
      return null;
    }

    List<String> sliMetricNames = serviceLevelIndicator.getMetricNames();
    List<CustomHealthDataCollectionInfo.CustomHealthMetricInfo> metricInfoList = new ArrayList<>();
    cvConfigs.forEach(cvConfig -> cvConfig.getMetricDefinitions().forEach(metricInfo -> {
      if (sliMetricNames.contains(metricInfo.getMetricName())) {
        metricInfoList.add(mapMetricDefinitionToMetricInfo(metricInfo));
      }
    }));

    CustomHealthDataCollectionInfo customHealthDataCollectionInfo = CustomHealthDataCollectionInfo.builder()
                                                                        .groupName(cvConfigs.get(0).getGroupName())
                                                                        .metricInfoList(metricInfoList)
                                                                        .build();
    customHealthDataCollectionInfo.setDataCollectionDsl(cvConfigs.get(0).getDataCollectionDsl());
    return customHealthDataCollectionInfo;
  }

  private CustomHealthDataCollectionInfo.CustomHealthMetricInfo mapMetricDefinitionToMetricInfo(
      CustomHealthCVConfig.MetricDefinition metricDefinition) {
    MetricResponseMapping metricResponseMapping = metricDefinition.getMetricResponseMapping();
    return CustomHealthDataCollectionInfo.CustomHealthMetricInfo.builder()
        .metricName(metricDefinition.getMetricName())
        .endTime(metricDefinition.getEndTime())
        .responseMapping(MetricResponseMappingDTO.builder()
                             .metricValueJsonPath(metricResponseMapping.getMetricValueJsonPath())
                             .serviceInstanceJsonPath(metricResponseMapping.getServiceInstanceJsonPath())
                             .timestampFormat(metricResponseMapping.getTimestampFormat())
                             .timestampJsonPath(metricResponseMapping.getTimestampJsonPath())
                             .build())
        .body(metricDefinition.getRequestBody())
        .method(metricDefinition.getMethod())
        .startTime(metricDefinition.getStartTime())
        .urlPath(metricDefinition.getUrlPath())
        .build();
  }
}
