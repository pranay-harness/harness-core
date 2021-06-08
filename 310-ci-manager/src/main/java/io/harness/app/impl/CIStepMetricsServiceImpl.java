package io.harness.app.impl;

import io.harness.app.beans.dto.StepResourceMetricDTO;
import io.harness.app.intfc.CIStepMetricsService;
import io.harness.util.MetricCache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class CIStepMetricsServiceImpl implements CIStepMetricsService {
  @Inject MetricCache metricCache;

  @Override
  public List<StepResourceMetricDTO> getCIStepMetrics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    List<StepResourceMetricDTO> stepResourceMetricDTOList = new ArrayList<>();

    for (String stageStepId :
        getPipelineSteps(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier)) {
      String[] split = stageStepId.split(":", 2);
      String stageId = split[0];
      String stepId = split[1];
      StepResourceMetricDTO cpuMemNode =
          getStepMetrics(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, stageId, stepId);

      stepResourceMetricDTOList.add(cpuMemNode);
    }
    return stepResourceMetricDTOList;
  }

  private List<String> getPipelineSteps(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return metricCache.getPipelineSteps(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier);
  }

  private StepResourceMetricDTO getStepMetrics(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String stageId, String stepId) {
    int maxStepMilliCpu = metricCache.getMaxStepMilliCpu(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, stageId, stepId);
    int maxStepMemoryMib = metricCache.getMaxStepMemoryMib(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, stageId, stepId);
    int currStepMemoryMib = metricCache.getCurrStepMemoryMib(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, stageId, stepId);
    int currStepMilliCpu = metricCache.getCurrStepMilliCpu(
        accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier, stageId, stepId);

    int suggestedStepMemoryMib = (int) Math.ceil((double) maxStepMemoryMib * 1.2);
    int suggestedStepMilliCpu = (int) Math.ceil((double) maxStepMilliCpu * 1.2);

    return StepResourceMetricDTO.builder()
        .currentMilliCpu(currStepMilliCpu)
        .suggestedMilliCpu(suggestedStepMilliCpu)
        .maxMilliCpu(maxStepMilliCpu)
        .currentMemoryMib(currStepMemoryMib)
        .suggestedMemoryMib(suggestedStepMilliCpu)
        .maxMemoryMib(maxStepMemoryMib)
        .isCpuOverProvisioned(currStepMilliCpu > suggestedStepMilliCpu)
        .isMemoryOverProvisioned(currStepMemoryMib > suggestedStepMemoryMib)
        .stageId(stageId)
        .stepId(stepId)
        .build();
  }
}
