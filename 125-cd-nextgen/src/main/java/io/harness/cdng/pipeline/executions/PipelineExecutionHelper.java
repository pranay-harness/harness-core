package io.harness.cdng.pipeline.executions;

import static io.harness.pms.execution.ExecutionStatus.getExecutionStatus;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.UnknownStageElementWrapperException;
import io.harness.execution.NodeExecution;
import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary.CDStageExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.ParallelStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.ParallelStageExecutionSummary.ParallelStageExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.registries.StageTypeToStageExecutionMapperHelperRegistry;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionErrorInfo;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.mongodb.core.query.Update;

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

  public CDStageExecutionSummary getCDStageExecutionSummary(NodeExecution nodeExecution) {
    CDStageExecutionSummary cdStageExecutionSummary = CDStageExecutionSummary.builder().build();
    ExecutionStatus executionStatus = getExecutionStatus(nodeExecution.getStatus());
    cdStageExecutionSummary.setExecutionStatus(executionStatus);
    cdStageExecutionSummary.setNodeExecutionId(nodeExecution.getUuid());

    cdStageExecutionSummary.setStartedAt(nodeExecution.getStartTs());
    if (ExecutionStatus.isTerminal(executionStatus)) {
      cdStageExecutionSummary.setEndedAt(nodeExecution.getEndTs());
      if (nodeExecution.getFailureInfo() != null) {
        cdStageExecutionSummary.setErrorInfo(
            ExecutionErrorInfo.builder().message(nodeExecution.getFailureInfo().getErrorMessage()).build());
      }
    }
    return cdStageExecutionSummary;
  }

  public Update getCDStageExecutionSummaryStatusUpdate(StageIndex stageIndex, NodeExecution nodeExecution) {
    String key = getKeyForGivenStage(stageIndex);

    ExecutionStatus executionStatus = getExecutionStatus(nodeExecution.getStatus());
    Update update = new Update();
    update.set(key + "." + CDStageExecutionSummaryKeys.executionStatus, executionStatus)
        .set(key + "." + CDStageExecutionSummaryKeys.nodeExecutionId, nodeExecution.getUuid())
        .set(key + "." + CDStageExecutionSummaryKeys.startedAt, nodeExecution.getStartTs());

    if (ExecutionStatus.isTerminal(executionStatus)) {
      update.set(key + "." + CDStageExecutionSummaryKeys.endedAt, nodeExecution.getEndTs());
      if (nodeExecution.getFailureInfo() != null) {
        update.set(key + "." + CDStageExecutionSummaryKeys.errorInfo, nodeExecution.getFailureInfo());
      }
    }
    return update;
  }

  public Update getCDStageExecutionSummaryServiceUpdate(
      StageIndex stageIndex, ServiceExecutionSummary serviceExecutionSummary) {
    String key = getKeyForGivenStage(stageIndex);
    Update update = new Update();
    update.set(key + "." + CDStageExecutionSummaryKeys.serviceIdentifier, serviceExecutionSummary.getIdentifier())
        .set(key + "." + CDStageExecutionSummaryKeys.serviceExecutionSummary, serviceExecutionSummary)
        .addToSet(PipelineExecutionSummaryKeys.serviceIdentifiers, serviceExecutionSummary.getIdentifier());
    return update;
  }

  public Update getCDStageExecutionSummaryEnvironmentUpdate(
      StageIndex stageIndex, EnvironmentOutcome environmentOutcome) {
    String key = getKeyForGivenStage(stageIndex);
    Update update = new Update();
    update.set(key + "." + CDStageExecutionSummaryKeys.envIdentifier, environmentOutcome.getIdentifier())
        .addToSet(PipelineExecutionSummaryKeys.envIdentifiers, environmentOutcome.getIdentifier());
    return update;
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
        CDStageExecutionSummary matchingCDStageExecutionSummary = findStageExecutionSummaryByNodeExecutionId(
            parallelStageExecutionSummary.getStageExecutionSummaries(), nodeExecutionId);

        if (matchingCDStageExecutionSummary != null) {
          return matchingCDStageExecutionSummary;
        }
      } else if (stageExecutionSummary instanceof CDStageExecutionSummary
          && ((CDStageExecutionSummary) stageExecutionSummary).getNodeExecutionId().equals(nodeExecutionId)) {
        return (CDStageExecutionSummary) stageExecutionSummary;
      }
    }
    return null;
  }

  public StageIndex findStageIndexByNodeExecutionId(
      List<StageExecutionSummary> stageExecutionSummaries, String nodeExecutionId) {
    int index = 0;
    for (StageExecutionSummary stageExecutionSummary : stageExecutionSummaries) {
      if (stageExecutionSummary instanceof ParallelStageExecutionSummary) {
        ParallelStageExecutionSummary parallelStageExecutionSummary =
            (ParallelStageExecutionSummary) stageExecutionSummary;
        StageIndex stageIndex = findStageIndexByNodeExecutionId(
            parallelStageExecutionSummary.getStageExecutionSummaries(), nodeExecutionId);

        if (stageIndex != null) {
          return StageIndex.builder().firstLevelIndex(index).secondLevelIndex(stageIndex.firstLevelIndex).build();
        }
      } else if (stageExecutionSummary instanceof CDStageExecutionSummary
          && ((CDStageExecutionSummary) stageExecutionSummary).getNodeExecutionId().equals(nodeExecutionId)) {
        return StageIndex.builder().firstLevelIndex(index).build();
      }
      index++;
    }
    return null;
  }

  public StageIndex findStageIndexByPlanNodeId(List<StageExecutionSummary> stageExecutionSummaries, String planNodeId) {
    int index = 0;
    for (StageExecutionSummary stageExecutionSummary : stageExecutionSummaries) {
      if (stageExecutionSummary instanceof ParallelStageExecutionSummary) {
        ParallelStageExecutionSummary parallelStageExecutionSummary =
            (ParallelStageExecutionSummary) stageExecutionSummary;
        StageIndex stageIndex =
            findStageIndexByPlanNodeId(parallelStageExecutionSummary.getStageExecutionSummaries(), planNodeId);

        if (stageIndex != null) {
          return StageIndex.builder().firstLevelIndex(index).secondLevelIndex(stageIndex.firstLevelIndex).build();
        }
      } else if (stageExecutionSummary instanceof CDStageExecutionSummary
          && ((CDStageExecutionSummary) stageExecutionSummary).getPlanNodeId().equals(planNodeId)) {
        return StageIndex.builder().firstLevelIndex(index).build();
      }
      index++;
    }
    return null;
  }

  private String getKeyForGivenStage(StageIndex stageIndex) {
    return stageIndex.getSecondLevelIndex() != -1
        ? String.format("%s.%s.%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements,
            stageIndex.getFirstLevelIndex(), ParallelStageExecutionSummaryKeys.stageExecutionSummaries,
            stageIndex.getSecondLevelIndex())
        : String.format("%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements,
            stageIndex.getFirstLevelIndex());
  }

  @Value
  @Builder
  public static class StageIndex {
    @Builder.Default int firstLevelIndex = -1;
    @Builder.Default int secondLevelIndex = -1;
  }
}
