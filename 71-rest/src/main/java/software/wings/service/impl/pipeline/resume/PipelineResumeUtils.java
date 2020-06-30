package software.wings.service.impl.pipeline.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.isActiveStatus;
import static io.harness.beans.ExecutionStatus.isNegativeStatus;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.NOT_EXISTS;
import static io.harness.beans.SearchFilter.Operator.OR;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.APPROVAL_RESUME;
import static software.wings.sm.StateType.ENV_RESUME_STATE;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.PipelineStageGroupedInfo;
import software.wings.beans.PipelineStageGroupedInfo.PipelineStageGroupedInfoBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.auth.DeploymentAuthHandler;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.EnvResumeState.EnvResumeStateKeys;
import software.wings.sm.states.EnvState.EnvStateKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class PipelineResumeUtils {
  private static final String PIPELINE_RESUME_PIPELINE_CHANGED = "You cannot resume a pipeline which has been modified";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PipelineService pipelineService;
  @Inject private DeploymentAuthHandler deploymentAuthHandler;
  @Inject private FeatureFlagService featureFlagService;

  public Pipeline getPipelineForResume(String appId, int parallelIndexToResume, WorkflowExecution prevWorkflowExecution,
      ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap) {
    checkPipelineResumeAvailable(prevWorkflowExecution);
    deploymentAuthHandler.authorize(appId, prevWorkflowExecution);
    String pipelineId = prevWorkflowExecution.getWorkflowId();
    ExecutionArgs executionArgs = prevWorkflowExecution.getExecutionArgs();
    Pipeline pipeline = pipelineService.readPipelineWithResolvedVariables(
        appId, pipelineId, executionArgs.getWorkflowVariables(), true);
    if (pipeline == null) {
      throw new InvalidRequestException("Pipeline does not exist");
    }
    if (isEmpty(pipeline.getPipelineStages())) {
      throw new InvalidRequestException("You cannot resume an empty pipeline");
    }

    updatePipelineForResume(pipeline, parallelIndexToResume, prevWorkflowExecution, stateExecutionInstanceMap);
    return pipeline;
  }

  private void updatePipelineForResume(Pipeline pipeline, int parallelIndexToResume,
      WorkflowExecution prevWorkflowExecution, ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap) {
    List<PipelineStageExecution> pipelineStageExecutions =
        prevWorkflowExecution.getPipelineExecution().getPipelineStageExecutions();
    if (isEmpty(pipelineStageExecutions)) {
      // Previous pipeline execution had 0 stages.
      throw new InvalidRequestException(PIPELINE_RESUME_PIPELINE_CHANGED);
    }

    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    for (int i = 0; i < pipelineStages.size(); i++) {
      PipelineStage pipelineStage = pipelineStages.get(i);
      if (pipelineStageExecutions.size() <= i) {
        // Previous pipeline execution did not have enough stages.
        throw new InvalidRequestException(PIPELINE_RESUME_PIPELINE_CHANGED);
      }

      List<PipelineStageElement> pipelineStageElements = pipelineStage.getPipelineStageElements();
      if (isEmpty(pipelineStageElements)) {
        continue;
      }

      PipelineStageExecution stageExecution = pipelineStageExecutions.get(i);
      // Check for compatibility.
      checkStageAndStageExecution(pipelineStage, stageExecution);

      int workflowExecutionIndex = -1;
      boolean foundResumeStage = false;
      for (PipelineStageElement pse : pipelineStageElements) {
        if (pse.getParallelIndex() >= parallelIndexToResume) {
          foundResumeStage = true;
          break;
        }
        if (ENV_STATE.name().equals(pse.getType())) {
          // This variable tracks which execution to pick from stageExecution.getWorkflowExecutions().
          workflowExecutionIndex++;
        }
        if (!ENV_STATE.name().equals(pse.getType()) && !APPROVAL.name().equals(pse.getType())) {
          // Return if the state is not EnvState or ApprovalState.
          continue;
        }

        StateExecutionInstance stateExecutionInstance = stateExecutionInstanceMap.get(pse.getName());
        StateExecutionData stateExecutionData = null;
        if (stateExecutionInstance != null) {
          stateExecutionData = stateExecutionInstance.fetchStateExecutionData();
        }
        if (stateExecutionInstance == null
            || (ENV_STATE.name().equals(pse.getType()) && !(stateExecutionData instanceof EnvStateExecutionData))
            || (APPROVAL.name().equals(pse.getType()) && !(stateExecutionData instanceof ApprovalStateExecutionData))) {
          throw new InvalidRequestException(
              format("You cannot resume a pipeline which has been modified. New stage [%s] introduced in the pipeline",
                  pse.getName()));
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(EnvResumeStateKeys.prevStateExecutionId, stateExecutionInstance.getUuid());
        properties.put(EnvResumeStateKeys.prevPipelineExecutionId, prevWorkflowExecution.getUuid());
        if (ENV_STATE.name().equals(pse.getType())) {
          pse.setType(ENV_RESUME_STATE.name());
          List<WorkflowExecution> workflowExecutions = stageExecution.getWorkflowExecutions();
          List<String> workflowExecutionIds;
          if (isEmpty(workflowExecutions) || workflowExecutionIndex < 0
              || workflowExecutions.size() <= workflowExecutionIndex) {
            // This might happen in case a pipeline stage is skipped.
            workflowExecutionIds = new ArrayList<>();
          } else {
            workflowExecutionIds = Collections.singletonList(workflowExecutions.get(workflowExecutionIndex).getUuid());
          }
          properties.put(EnvResumeStateKeys.prevWorkflowExecutionIds, workflowExecutionIds);
        } else {
          pse.setType(APPROVAL_RESUME.name());
        }

        pse.setProperties(properties);
      }

      if (foundResumeStage) {
        break;
      }
    }
  }

  public void updatePipelineExecutionsAfterResume(
      WorkflowExecution currWorkflowExecution, WorkflowExecution prevWorkflowExecution) {
    String pipelineResumeId = prevWorkflowExecution.getPipelineResumeId();
    if (isEmpty(pipelineResumeId)) {
      pipelineResumeId = prevWorkflowExecution.getUuid();
    }

    // NOTE: The below operations should ideally be atomic but we don't have multi-document transactions yet.
    UpdateOperations<WorkflowExecution> currOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    currOps.set(WorkflowExecutionKeys.pipelineResumeId, pipelineResumeId);
    currOps.set(WorkflowExecutionKeys.latestPipelineResume, Boolean.TRUE);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter(WorkflowExecutionKeys.appId, currWorkflowExecution.getAppId())
                                .filter(WorkflowExecutionKeys.uuid, currWorkflowExecution.getUuid()),
        currOps);
    currWorkflowExecution.setPipelineResumeId(pipelineResumeId);
    currWorkflowExecution.setLatestPipelineResume(true);

    UpdateOperations<WorkflowExecution> prevOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    prevOps.set(WorkflowExecutionKeys.pipelineResumeId, pipelineResumeId);
    prevOps.set(WorkflowExecutionKeys.latestPipelineResume, Boolean.FALSE);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter(WorkflowExecutionKeys.appId, prevWorkflowExecution.getAppId())
                                .filter(WorkflowExecutionKeys.uuid, prevWorkflowExecution.getUuid()),
        prevOps);
    prevWorkflowExecution.setPipelineResumeId(pipelineResumeId);
    prevWorkflowExecution.setLatestPipelineResume(false);
  }

  public List<PipelineStageGroupedInfo> getResumeStages(String appId, WorkflowExecution prevWorkflowExecution) {
    checkPipelineResumeAvailable(prevWorkflowExecution);
    String pipelineId = prevWorkflowExecution.getWorkflowId();
    ExecutionArgs executionArgs = prevWorkflowExecution.getExecutionArgs();
    Pipeline pipeline = pipelineService.readPipelineWithResolvedVariables(
        appId, pipelineId, executionArgs.getWorkflowVariables(), true);
    if (pipeline == null) {
      throw new InvalidRequestException("Pipeline does not exist");
    }

    List<PipelineStage> pipelineStages = pipeline.getPipelineStages();
    if (isEmpty(pipelineStages)) {
      throw new InvalidRequestException("You cannot resume an empty pipeline");
    }

    List<PipelineStageExecution> pipelineStageExecutions =
        prevWorkflowExecution.getPipelineExecution().getPipelineStageExecutions();
    if (isEmpty(pipelineStageExecutions)) {
      // Previous pipeline execution had 0 stages.
      throw new InvalidRequestException(PIPELINE_RESUME_PIPELINE_CHANGED);
    }

    boolean foundFailedStage = false;
    List<PipelineStageGroupedInfoBuilder> groupedInfoBuilders = new ArrayList<>();
    for (int i = 0; i < pipelineStages.size(); i++) {
      if (pipelineStageExecutions.size() <= i) {
        // Previous pipeline execution did not have enough stages.
        throw new InvalidRequestException(PIPELINE_RESUME_PIPELINE_CHANGED);
      }

      PipelineStage pipelineStage = pipelineStages.get(i);
      if (foundFailedStage && !pipelineStage.isParallel()) {
        // We already found a failed stage and the new stage is not parallel with previous ones. So we stop our
        // iteration.
        break;
      }

      boolean isNegativeStatus = false;
      PipelineStageExecution stageExecution;
      if (featureFlagService.isEnabled(FeatureName.MULTISELECT_INFRA_PIPELINE, pipeline.getAccountId())) {
        List<PipelineStageExecution> stageExecutions =
            pipelineStageExecutions.stream()
                .filter(t
                    -> t.getPipelineStageElementId().equals(pipelineStage.getPipelineStageElements().get(0).getUuid()))
                .collect(Collectors.toList());
        if (isEmpty(stageExecutions)) {
          // older flow when we didnt add pipeline stage element Id. Can be cleaned up after 6 months. Date:
          // 30-June-2020
          stageExecution = pipelineStageExecutions.get(i);
          checkStageAndStageExecution(pipelineStage, stageExecution);
          if (isNegativeStatus(stageExecution.getStatus())) {
            isNegativeStatus = true;
          }
        } else {
          if (stageExecutions.size() > 1 && stageExecutions.stream().anyMatch(t -> !t.isLooped())) {
            throw new InvalidRequestException(PIPELINE_RESUME_PIPELINE_CHANGED);
          }
          for (PipelineStageExecution t : stageExecutions) {
            checkStageAndStageExecution(pipelineStage, t);
            if (isNegativeStatus(t.getStatus())) {
              isNegativeStatus = true;
              break;
            }
          }
        }
      } else {
        stageExecution = pipelineStageExecutions.get(i);
        // Check for compatibility.
        checkStageAndStageExecution(pipelineStage, stageExecution);
        if (isNegativeStatus(stageExecution.getStatus())) {
          isNegativeStatus = true;
        }
      }

      List<String> newPipelineStageElementNames = pipelineStage.getPipelineStageElements() == null
          ? Collections.emptyList()
          : pipelineStage.getPipelineStageElements()
                .stream()
                .map(pse -> {
                  if (APPROVAL.name().equals(pse.getType())) {
                    return "Approval";
                  }
                  return pse.getName();
                })
                .collect(Collectors.toList());
      if (pipelineStage.isParallel() && isNotEmpty(groupedInfoBuilders)) {
        // The stage is parallel to the previous one. Just append the new element names to the last group info.
        groupedInfoBuilders.get(groupedInfoBuilders.size() - 1).pipelineStageElementNames(newPipelineStageElementNames);
      } else {
        // The stage is not parallel the to previous one. We have to construct a new group info with the correct name
        // and parallel index.
        PipelineStageGroupedInfoBuilder builder = PipelineStageGroupedInfo.builder()
                                                      .name(pipelineStage.getName())
                                                      .pipelineStageElementNames(newPipelineStageElementNames);
        if (isNotEmpty(pipelineStage.getPipelineStageElements())) {
          builder.parallelIndex(pipelineStage.getPipelineStageElements().get(0).getParallelIndex());
        }
        groupedInfoBuilders.add(builder);
      }

      if (isNegativeStatus) {
        // Found a non-successful stage in a FAILED pipeline. We will now continue only till we find a stages which are
        // parallel with the current one.
        foundFailedStage = true;
      }
    }

    return groupedInfoBuilders.stream().map(PipelineStageGroupedInfoBuilder::build).collect(Collectors.toList());
  }

  public List<WorkflowExecution> getResumeHistory(String appId, WorkflowExecution prevWorkflowExecution) {
    checkPipelineResumeHistoryAvailable(prevWorkflowExecution);
    String pipelineResumeId = prevWorkflowExecution.getPipelineResumeId();
    if (isEmpty(pipelineResumeId)) {
      return new ArrayList<>();
    }

    List<WorkflowExecution> workflowExecutions = wingsPersistence.createQuery(WorkflowExecution.class)
                                                     .filter(WorkflowExecutionKeys.appId, appId)
                                                     .filter(WorkflowExecutionKeys.pipelineResumeId, pipelineResumeId)
                                                     .project(WorkflowExecutionKeys.stateMachine, false)
                                                     .asList();
    if (isEmpty(workflowExecutions)) {
      return workflowExecutions;
    }

    workflowExecutions.sort(Comparator.comparingLong(WorkflowExecution::getCreatedAt));
    return workflowExecutions;
  }

  @VisibleForTesting
  void checkPipelineResumeAvailable(WorkflowExecution prevWorkflowExecution) {
    if (prevWorkflowExecution.getWorkflowType() != PIPELINE) {
      throw new InvalidRequestException(
          format("Pipeline resume not available for workflow executions: %s", prevWorkflowExecution.getUuid()));
    }
    if (prevWorkflowExecution.getStatus() != FAILED) {
      throw new InvalidRequestException(
          format("Pipeline resume is not available for non failed executions: %s", prevWorkflowExecution.getUuid()));
    }
    if (isNotEmpty(prevWorkflowExecution.getPipelineResumeId()) && !prevWorkflowExecution.isLatestPipelineResume()) {
      throw new InvalidRequestException(
          format("Pipeline resume is only available on the latest iteration of a resumed execution: %s",
              prevWorkflowExecution.getUuid()));
    }

    if (prevWorkflowExecution.getStatus() == FAILED) {
      List<PipelineStageExecution> pipelineStageExecutions =
          prevWorkflowExecution.getPipelineExecution().getPipelineStageExecutions();
      if (isEmpty(pipelineStageExecutions)) {
        throw new InvalidRequestException("You cannot resume an empty pipeline");
      } else {
        boolean notInActiveStatus = false;
        for (PipelineStageExecution pipelineStageExecution : pipelineStageExecutions) {
          if (!isActiveStatus(pipelineStageExecution.getStatus())) {
            notInActiveStatus = true;
            break;
          }
        }
        if (!notInActiveStatus) {
          throw new InvalidRequestException(
              "Pipeline resume is not available for a pipeline that failed during artifact collection");
        }
      }
    }
  }

  @VisibleForTesting
  void checkPipelineResumeHistoryAvailable(WorkflowExecution prevWorkflowExecution) {
    if (prevWorkflowExecution.getWorkflowType() != PIPELINE) {
      throw new InvalidRequestException(
          format("Pipeline resume not available for workflow executions: %s", prevWorkflowExecution.getUuid()));
    }
  }

  /***
   * checkStageAndStageExecution checks if the stage and stageExecution contain the same workflows/executions with the
   * same workflow ids. If a new workflow is introduced or an old one removed from a pipeline, the stage executions will
   * differ.
   */
  @VisibleForTesting
  void checkStageAndStageExecution(PipelineStage stage, PipelineStageExecution stageExecution) {
    if (stageExecution.getStatus() == SKIPPED) {
      // Don't check for skipped stage executions as they have no workflow executions attached to them.
      return;
    }

    List<PipelineStageElement> stageElements = emptyIfNull(stage.getPipelineStageElements())
                                                   .stream()
                                                   .filter(pse -> ENV_STATE.name().equals(pse.getType()))
                                                   .collect(Collectors.toList());
    List<WorkflowExecution> workflowExecutions = emptyIfNull(stageExecution.getWorkflowExecutions());

    Set<String> newWorkflowIds = stageElements.stream()
                                     .map(pse -> (String) pse.getProperties().get(EnvStateKeys.workflowId))
                                     .filter(Objects::nonNull)
                                     .collect(Collectors.toSet());
    Map<String, WorkflowExecution> oldWorkflowIdMap =
        workflowExecutions.stream()
            .filter(execution -> execution != null && execution.getWorkflowId() != null)
            .collect(Collectors.toMap(WorkflowExecution::getWorkflowId, Function.identity()));

    Set<String> diff = new HashSet<>(oldWorkflowIdMap.keySet());
    diff.removeAll(newWorkflowIds);
    if (isNotEmpty(diff)) {
      String workflowId = diff.iterator().next();
      String workflowName = oldWorkflowIdMap.get(workflowId).getName();
      throw new InvalidRequestException(format(
          "You cannot resume a pipeline which has been modified. Pipeline stage [%s] modified and a workflow [%s] has been removed.",
          stage.getName(), isEmpty(workflowName) ? workflowId : workflowName));
    }

    diff = new HashSet<>(newWorkflowIds);
    diff.removeAll(oldWorkflowIdMap.keySet());
    if (isNotEmpty(diff)) {
      throw new InvalidRequestException(format(
          "You cannot resume a pipeline which has been modified. Pipeline stage [%s] modified and a workflow [%s] has been added.",
          stage.getName(), diff.iterator().next()));
    }
  }

  public static void addLatestPipelineResumeFilter(PageRequest<WorkflowExecution> pageRequest) {
    pageRequest.addFilter("", OR,
        SearchFilter.builder().fieldName(WorkflowExecutionKeys.pipelineResumeId).op(NOT_EXISTS).build(),
        SearchFilter.builder()
            .fieldName(WorkflowExecutionKeys.latestPipelineResume)
            .op(EQ)
            .fieldValues(new Object[] {Boolean.TRUE})
            .build());
  }
}
