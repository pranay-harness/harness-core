package io.harness.cdng.pipeline.executions;

import static io.harness.cdng.pipeline.executions.ExecutionStatus.getExecutionStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.cdng.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.ParallelStageExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.StageExecutionSummary;
import io.harness.cdng.pipeline.executions.registries.StageTypeToStageExecutionMapperHelperRegistry;
import io.harness.exception.UnknownStageElementWrapperException;
import io.harness.execution.NodeExecution;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class PipelineExecutionHelper {
  @Inject StageTypeToStageExecutionMapperHelperRegistry stageTypeToStageExecutionMapperHelperRegistry;

  public void addStageSpecificDetailsToPipelineExecution(PipelineExecutionSummary pipelineExecutionSummary,
      CDPipeline cdPipeline, Map<String, String> stageIdentifierToPlanNodeId) {
    for (StageElementWrapper stageElementWrapper : cdPipeline.getStages()) {
      addStageElementDetailsBasedOnStageElementWrapperType(
          stageElementWrapper, stageIdentifierToPlanNodeId, pipelineExecutionSummary, false);
    }
  }

  private StageExecutionSummary addStageElementDetailsBasedOnStageElementWrapperType(
      StageElementWrapper stageElementWrapper, Map<String, String> stageIdentifierToPlanNodeId,
      PipelineExecutionSummary pipelineExecutionSummary, boolean skipAddingStageExecutionElement) {
    if (stageElementWrapper instanceof ParallelStageElement) {
      StageExecutionSummary stageExecutionSummary = mapParellelStageElementToStageExecution(
          (ParallelStageElement) stageElementWrapper, stageIdentifierToPlanNodeId, pipelineExecutionSummary);
      pipelineExecutionSummary.addStageExecutionSummaryElement(stageExecutionSummary);
      return stageExecutionSummary;
    } else if (stageElementWrapper instanceof StageElement) {
      StageType stageType = ((StageElement) stageElementWrapper).getStageType();
      StageTypeToStageExecutionSummaryMapper stageTypeToStageExecutionSummaryMapper =
          stageTypeToStageExecutionMapperHelperRegistry.obtain(stageType.getStageType());
      StageExecutionSummary stageExecutionSummary = stageTypeToStageExecutionSummaryMapper.getStageExecution(stageType,
          stageIdentifierToPlanNodeId.get(stageType.getIdentifier()), pipelineExecutionSummary.getPlanExecutionId());
      if (!skipAddingStageExecutionElement) {
        pipelineExecutionSummary.addStageExecutionSummaryElement(stageExecutionSummary);
      }
      pipelineExecutionSummary.addEnvironmentIdentifier(
          stageTypeToStageExecutionSummaryMapper.getEnvironmentIdentifier(stageType));
      pipelineExecutionSummary.addServiceIdentifier(
          stageTypeToStageExecutionSummaryMapper.getServiceIdentifier(stageType));
      pipelineExecutionSummary.addStageIdentifier(stageType.getIdentifier());
      pipelineExecutionSummary.addNGStageType(stageType.getStageType());
      pipelineExecutionSummary.addServiceDefinitionType(
          stageTypeToStageExecutionSummaryMapper.getServiceDefinitionType(stageType));
      return stageExecutionSummary;
    }
    throw new UnknownStageElementWrapperException();
  }

  private StageExecutionSummary mapParellelStageElementToStageExecution(ParallelStageElement parallelStageElement,
      Map<String, String> stageIdentifierToPlanNodeId, PipelineExecutionSummary pipelineExecutionSummary) {
    List<StageExecutionSummary> stageExecutionSummaries = new ArrayList<>();
    parallelStageElement.getSections().forEach(stageElementWrapper -> {
      stageExecutionSummaries.add(addStageElementDetailsBasedOnStageElementWrapperType(
          stageElementWrapper, stageIdentifierToPlanNodeId, pipelineExecutionSummary, true));
    });
    return ParallelStageExecutionSummary.builder().stageExecutionSummaries(stageExecutionSummaries).build();
  }

  public void updateStageExecutionStatus(
      PipelineExecutionSummary pipelineExecutionSummary, NodeExecution nodeExecution) {
    CDStageExecutionSummary cdStageExecutionSummary = getStageExecutionSummaryToBeUpdated(
        pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), nodeExecution.getNode().getUuid());
    if (cdStageExecutionSummary == null) {
      return;
    }
    ExecutionStatus executionStatus = getExecutionStatus(nodeExecution.getStatus());
    cdStageExecutionSummary.setExecutionStatus(executionStatus);
    if (ExecutionStatus.isTerminal(executionStatus)) {
      cdStageExecutionSummary.setEndedAt(nodeExecution.getEndTs());
    } else {
      cdStageExecutionSummary.setStartedAt(nodeExecution.getStartTs());
    }
  }

  public void updatePipelineExecutionStatus(
      PipelineExecutionSummary pipelineExecutionSummary, NodeExecution nodeExecution) {
    ExecutionStatus executionStatus = getExecutionStatus(nodeExecution.getStatus());
    pipelineExecutionSummary.setExecutionStatus(executionStatus);
    if (ExecutionStatus.isTerminal(executionStatus)) {
      pipelineExecutionSummary.setEndedAt(nodeExecution.getEndTs());
    } else {
      pipelineExecutionSummary.setStartedAt(nodeExecution.getStartTs());
    }
  }

  private CDStageExecutionSummary getStageExecutionSummaryToBeUpdated(
      List<StageExecutionSummary> stageExecutionSummaries, String planNodeId) {
    for (StageExecutionSummary stageExecutionSummary : stageExecutionSummaries) {
      if (stageExecutionSummary instanceof ParallelStageExecutionSummary) {
        ParallelStageExecutionSummary parallelStageExecutionSummary =
            (ParallelStageExecutionSummary) stageExecutionSummary;
        return getStageExecutionSummaryToBeUpdated(
            parallelStageExecutionSummary.getStageExecutionSummaries(), planNodeId);
      } else if (stageExecutionSummary instanceof CDStageExecutionSummary
          && ((CDStageExecutionSummary) stageExecutionSummary).getPlanNodeId().equals(planNodeId)) {
        return (CDStageExecutionSummary) stageExecutionSummary;
      }
    }
    return null;
  }
}
