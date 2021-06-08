package io.harness.app.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

import io.harness.app.beans.dto.StepResourceMetricDTO;
import io.harness.app.intfc.CIStepMetricsService;
import io.harness.util.MetricCache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class CIStepMetricsServiceImpl implements CIStepMetricsService {
  public static final double SCALING_COEFFICIENT = 1.5;
  public static final int MIN_SUGGESTION_VALUE_MEM = 100;
  public static final int MIN_SUGGESTION_VALUE_CPU = 100;

  @Inject MetricCache metricCache;

  @Override
  public List<StepResourceMetricDTO> getCIStepMetrics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    List<StepResourceMetricDTO> stepResourceMetricDTOList = new ArrayList<>();

    List<String> pipelineSteps =
        getPipelineSteps(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier);

    if (isEmpty(pipelineSteps)) {
      return stepResourceMetricDTOList;
    }

    for (String stageStepId : pipelineSteps) {
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

    int suggestedStepMemoryMib =
        max((int) ceil(((double) maxStepMemoryMib) * SCALING_COEFFICIENT), MIN_SUGGESTION_VALUE_MEM);
    int suggestedStepMilliCpu =
        max((int) ceil(((double) maxStepMilliCpu) * SCALING_COEFFICIENT), MIN_SUGGESTION_VALUE_CPU);

    return StepResourceMetricDTO.builder()
        .currentMilliCpu(currStepMilliCpu)
        .suggestedMilliCpu(suggestedStepMilliCpu)
        .maxMilliCpu(maxStepMilliCpu)
        .isCpuOverProvisioned(currStepMilliCpu > suggestedStepMilliCpu)
        .currentMemoryMib(currStepMemoryMib)
        .suggestedMemoryMib(suggestedStepMemoryMib)
        .maxMemoryMib(maxStepMemoryMib)
        .isMemoryOverProvisioned(currStepMemoryMib > suggestedStepMemoryMib)
        .stageId(stageId)
        .stepId(stepId)
        .build();
  }
}
