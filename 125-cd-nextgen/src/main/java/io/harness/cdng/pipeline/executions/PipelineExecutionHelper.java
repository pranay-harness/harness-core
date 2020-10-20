package io.harness.cdng.pipeline.executions;

import static io.harness.ngpipeline.pipeline.executions.ExecutionStatus.getExecutionStatus;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.UnknownStageElementWrapperException;
import io.harness.execution.NodeExecution;
import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.executions.ExecutionStatus;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.ExecutionErrorInfo;
import io.harness.ngpipeline.pipeline.executions.beans.ParallelStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.registries.StageTypeToStageExecutionMapperHelperRegistry;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class PipelineExecutionHelper {
  @Inject StageTypeToStageExecutionMapperHelperRegistry stageTypeToStageExecutionMapperHelperRegistry;

  public void addStageSpecificDetailsToPipelineExecution(PipelineExecutionSummary pipelineExecutionSummary,
      NgPipeline ngPipeline, Map<String, String> stageIdentifierToPlanNodeId) {
    for (StageElementWrapper stageElementWrapper : ngPipeline.getStages()) {
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
      pipelineExecutionSummary.addStageIdentifier(stageType.getIdentifier());
      pipelineExecutionSummary.addNGStageType(stageType.getStageType());
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
    CDStageExecutionSummary cdStageExecutionSummary = findStageExecutionSummaryByPlanNodeId(
        pipelineExecutionSummary.getStageExecutionSummarySummaryElements(), nodeExecution.getNode().getUuid());
    if (cdStageExecutionSummary == null) {
      return;
    }
    ExecutionStatus executionStatus = getExecutionStatus(nodeExecution.getStatus());
    cdStageExecutionSummary.setExecutionStatus(executionStatus);
    if (cdStageExecutionSummary.getNodeExecutionId() == null) {
      cdStageExecutionSummary.setNodeExecutionId(nodeExecution.getUuid());
    }
    if (ExecutionStatus.isTerminal(executionStatus)) {
      cdStageExecutionSummary.setEndedAt(nodeExecution.getEndTs());
      if (nodeExecution.getFailureInfo() != null) {
        cdStageExecutionSummary.setErrorInfo(

            ExecutionErrorInfo.builder().message(nodeExecution.getFailureInfo().getErrorMessage()).build());
      }
    } else {
      cdStageExecutionSummary.setStartedAt(nodeExecution.getStartTs());
    }
  }

  public ServiceExecutionSummary.ArtifactsSummary mapArtifactsOutcomeToSummary(ServiceOutcome serviceOutcome) {
    ServiceExecutionSummary.ArtifactsSummary.ArtifactsSummaryBuilder artifactsSummaryBuilder =
        ServiceExecutionSummary.ArtifactsSummary.builder();

    if (serviceOutcome.getArtifacts().getPrimary() != null) {
      artifactsSummaryBuilder.primary(serviceOutcome.getArtifacts().getPrimary().getArtifactSummary());
    }

    if (EmptyPredicate.isNotEmpty(serviceOutcome.getArtifacts().getSidecars())) {
      artifactsSummaryBuilder.sidecars(serviceOutcome.getArtifacts()
                                           .getSidecars()
                                           .values()
                                           .stream()
                                           .filter(Objects::nonNull)
                                           .map(ArtifactOutcome::getArtifactSummary)
                                           .collect(Collectors.toList()));
    }

    return artifactsSummaryBuilder.build();
  }

  public void updatePipelineExecutionStatus(
      PipelineExecutionSummary pipelineExecutionSummary, NodeExecution nodeExecution) {
    ExecutionStatus executionStatus = getExecutionStatus(nodeExecution.getStatus());
    pipelineExecutionSummary.setExecutionStatus(executionStatus);
    if (ExecutionStatus.isTerminal(executionStatus)) {
      pipelineExecutionSummary.setEndedAt(nodeExecution.getEndTs());
      if (nodeExecution.getFailureInfo() != null) {
        pipelineExecutionSummary.setErrorInfo(
            ExecutionErrorInfo.builder().message(nodeExecution.getFailureInfo().getErrorMessage()).build());
      }
    } else {
      pipelineExecutionSummary.setStartedAt(nodeExecution.getStartTs());
    }
  }

  public CDStageExecutionSummary findStageExecutionSummaryByNodeExecutionId(
      List<StageExecutionSummary> stageExecutionSummaries, String nodeExecutionId) {
    for (StageExecutionSummary stageExecutionSummary : stageExecutionSummaries) {
      if (stageExecutionSummary instanceof ParallelStageExecutionSummary) {
        ParallelStageExecutionSummary parallelStageExecutionSummary =
            (ParallelStageExecutionSummary) stageExecutionSummary;
        return findStageExecutionSummaryByNodeExecutionId(
            parallelStageExecutionSummary.getStageExecutionSummaries(), nodeExecutionId);
      } else if (stageExecutionSummary instanceof CDStageExecutionSummary
          && ((CDStageExecutionSummary) stageExecutionSummary).getNodeExecutionId().equals(nodeExecutionId)) {
        return (CDStageExecutionSummary) stageExecutionSummary;
      }
    }
    return null;
  }

  public CDStageExecutionSummary findStageExecutionSummaryByPlanNodeId(
      List<StageExecutionSummary> stageExecutionSummaries, String planNodeId) {
    for (StageExecutionSummary stageExecutionSummary : stageExecutionSummaries) {
      if (stageExecutionSummary instanceof ParallelStageExecutionSummary) {
        ParallelStageExecutionSummary parallelStageExecutionSummary =
            (ParallelStageExecutionSummary) stageExecutionSummary;
        return findStageExecutionSummaryByPlanNodeId(
            parallelStageExecutionSummary.getStageExecutionSummaries(), planNodeId);
      } else if (stageExecutionSummary instanceof CDStageExecutionSummary
          && ((CDStageExecutionSummary) stageExecutionSummary).getPlanNodeId().equals(planNodeId)) {
        return (CDStageExecutionSummary) stageExecutionSummary;
      }
    }
    return null;
  }
}
