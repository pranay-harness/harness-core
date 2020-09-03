package io.harness.cvng.statemachine.entities;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.cvng.analysis.beans.ExecutionStatus;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@Slf4j
public class ServiceGuardLogClusterState extends AnalysisState {
  private LogClusterLevel clusterLevel;
  @JsonIgnore @Inject private transient LogClusterService logClusterService;
  private Set<String> workerTaskIds;
  private Map<String, ExecutionStatus> workerTaskStatus;

  @Override
  public AnalysisState execute() {
    List<String> taskIds = logClusterService.scheduleClusteringTasks(getInputs(), clusterLevel);
    if (isNotEmpty(taskIds)) {
      if (workerTaskIds == null) {
        workerTaskIds = new HashSet<>();
      }
      workerTaskIds.addAll(taskIds);
      this.setStatus(AnalysisStatus.RUNNING);
      logger.info(
          "Executing ServiceGuardLogClusterState for input: {}. Created {} tasks", getInputs(), workerTaskIds.size());
    } else {
      logger.info(
          "Executing ServiceGuardLogClusterState for input: {}. No clustering tasks were created. Marking as success",
          getInputs());
      this.setStatus(AnalysisStatus.SUCCESS);
    }
    return this;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (!getStatus().equals(AnalysisStatus.SUCCESS)) {
      Map<String, ExecutionStatus> taskStatuses =
          logClusterService.getTaskStatus(getInputs().getCvConfigId(), workerTaskIds);
      Map<ExecutionStatus, Set<String>> statusTaskMap = new HashMap<>();
      taskStatuses.forEach((taskId, taskStatus) -> {
        if (!statusTaskMap.containsKey(taskStatus)) {
          statusTaskMap.put(taskStatus, new HashSet<>());
        }
        statusTaskMap.get(taskStatus).add(taskId);
      });

      logger.info("Current statuses of worker tasks for ServiceGuardLogClusterState with inputs {} is {}", getInputs(),
          statusTaskMap);
      if (statusTaskMap.containsKey(ExecutionStatus.SUCCESS)
          && workerTaskIds.size() == statusTaskMap.get(ExecutionStatus.SUCCESS).size()) {
        logger.info("All worker tasks have succeeded.");
        return AnalysisStatus.TRANSITION;
      } else {
        if (statusTaskMap.containsKey(ExecutionStatus.RUNNING) || statusTaskMap.containsKey(ExecutionStatus.QUEUED)) {
          return AnalysisStatus.RUNNING;
        }
      }
    }
    return AnalysisStatus.TRANSITION;
  }

  @Override
  public AnalysisState handleRerun() {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleRunning() {
    return this;
  }

  @Override
  public AnalysisState handleSuccess() {
    this.setStatus(AnalysisStatus.SUCCESS);
    return this;
  }

  @Override
  public AnalysisState handleTransition() {
    this.setStatus(AnalysisStatus.SUCCESS);
    switch (clusterLevel) {
      case L1:
        AnalysisState nextState = ServiceGuardLogClusterState.builder().clusterLevel(LogClusterLevel.L2).build();
        nextState.setInputs(getInputs());
        nextState.setStatus(AnalysisStatus.CREATED);
        return nextState;
      case L2:
        nextState = ServiceGuardLogAnalysisState.builder().build();
        nextState.setInputs(getInputs());
        nextState.setStatus(AnalysisStatus.CREATED);
        return nextState;
      default:
        throw new AnalysisStateMachineException("Unknown cluster level in handleTransition "
            + "of ServiceGuardLogClusterState: " + clusterLevel);
    }
  }

  @Override
  public AnalysisState handleRetry() {
    // TODO: To be implemented
    return null;
  }
}
