package io.harness.cvng.statemachine.entities;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.LearningEngineTask.ExecutionStatus;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Data
@Slf4j
public abstract class LogClusterState extends AnalysisState {
  protected LogClusterLevel clusterLevel;
  @JsonIgnore @Inject protected transient LogClusterService logClusterService;
  private Set<String> workerTaskIds;
  private Map<String, ExecutionStatus> workerTaskStatus;

  protected abstract List<String> scheduleAnalysis(AnalysisInput analysisInput);
  @Override
  public AnalysisState execute() {
    List<String> taskIds = scheduleAnalysis(getInputs());
    if (isNotEmpty(taskIds)) {
      if (workerTaskIds == null) {
        workerTaskIds = new HashSet<>();
      }
      workerTaskIds.addAll(taskIds);
      this.setStatus(AnalysisStatus.RUNNING);
      log.info(
          "Executing ServiceGuardLogClusterState for input: {}. Created {} tasks", getInputs(), workerTaskIds.size());
    } else {
      log.error(
          "Executing ServiceGuardLogClusterState for input: {}. No clustering tasks were created. This is an error state",
          getInputs());
      throw new IllegalStateException("LogClusterState for input: " + getInputs()
          + ". No clustering tasks were created."
          + " This is an error state");
    }
    return this;
  }

  @Override
  public AnalysisStatus getExecutionStatus() {
    if (!getStatus().equals(AnalysisStatus.SUCCESS)) {
      Map<String, ExecutionStatus> taskStatuses = logClusterService.getTaskStatus(workerTaskIds);
      Map<ExecutionStatus, Set<String>> statusTaskMap = new HashMap<>();
      taskStatuses.forEach((taskId, taskStatus) -> {
        if (!statusTaskMap.containsKey(taskStatus)) {
          statusTaskMap.put(taskStatus, new HashSet<>());
        }
        statusTaskMap.get(taskStatus).add(taskId);
      });

      log.info("Current statuses of worker tasks with inputs {} is {}", getInputs(), statusTaskMap);
      if (statusTaskMap.containsKey(ExecutionStatus.SUCCESS)
          && workerTaskIds.size() == statusTaskMap.get(ExecutionStatus.SUCCESS).size()) {
        log.info("All worker tasks have succeeded.");
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
  public AnalysisState handleRetry() {
    // TODO: To be implemented
    return null;
  }
}
