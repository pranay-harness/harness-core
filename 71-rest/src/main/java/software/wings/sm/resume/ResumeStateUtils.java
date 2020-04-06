package software.wings.sm.resume;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.SweepingOutputInstance;
import io.harness.context.ContextElementType;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.service.impl.SweepingOutputServiceImpl;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ResumeStateUtils {
  public static final Integer RESUME_STATE_TIMEOUT_MILLIS = 60 * 1000;

  @Transient @Inject private StateExecutionService stateExecutionService;
  @Transient @Inject private SweepingOutputService sweepingOutputService;

  public ExecutionResponse prepareExecutionResponse(ExecutionContext context, String prevStateExecutionId) {
    StateExecutionInstance stateExecutionInstance =
        stateExecutionService.getStateExecutionData(context.getAppId(), prevStateExecutionId);
    notNullCheck("stateExecutionInstance is null", stateExecutionInstance);
    StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();
    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder()
                                                            .executionStatus(stateExecutionInstance.getStatus())
                                                            .errorMessage(stateExecutionData.getErrorMsg())
                                                            .stateExecutionData(stateExecutionData);
    if (isNotEmpty(stateExecutionInstance.getContextElements())) {
      // Copy context elements for build workflow.
      List<ContextElement> contextElements = stateExecutionInstance.getContextElements()
                                                 .stream()
                                                 .filter(el
                                                     -> el.getElementType() == ContextElementType.ARTIFACT
                                                         || el.getElementType() == ContextElementType.ARTIFACT_VARIABLE)
                                                 .collect(Collectors.toList());
      if (isNotEmpty(contextElements)) {
        executionResponseBuilder.contextElements(contextElements);
      }
    }
    return executionResponseBuilder.build();
  }

  public String fetchPipelineExecutionId(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams == null || workflowStandardParams.getWorkflowElement() == null
        ? null
        : workflowStandardParams.getWorkflowElement().getPipelineDeploymentUuid();
  }

  public void copyPipelineStageOutputs(String appId, String fromPipelineExecutionId, String fromStateExecutionId,
      List<String> fromWorkflowExecutionIds, String toPipelineExecutionId, String toStateExecutionId) {
    if (fromPipelineExecutionId.equals(toPipelineExecutionId)) {
      return;
    }

    List<SweepingOutputInstance> instances = new ArrayList<>();
    // Copy outputs from ApprovalState, ApprovalResumeState and EnvResumeState.
    try (
        HIterator<SweepingOutputInstance> instancesHIterator = new HIterator<>(
            sweepingOutputService.prepareApprovalStateOutputsQuery(appId, fromPipelineExecutionId, fromStateExecutionId)
                .fetch())) {
      for (SweepingOutputInstance instance : instancesHIterator) {
        instances.add(instance);
      }
    }

    // Copy outputs from EnvState.
    if (isNotEmpty(fromWorkflowExecutionIds)) {
      for (String fromWorkflowExecutionId : fromWorkflowExecutionIds) {
        try (HIterator<SweepingOutputInstance> instancesHIterator = new HIterator<>(
                 sweepingOutputService
                     .prepareEnvStateOutputsQuery(appId, fromPipelineExecutionId, fromWorkflowExecutionId)
                     .fetch())) {
          for (SweepingOutputInstance instance : instancesHIterator) {
            instances.add(instance);
          }
        }
      }
    }

    if (isEmpty(instances)) {
      return;
    }

    // Remove duplicates.
    Set<String> instanceIds = new HashSet<>();
    List<SweepingOutputInstance> newInstances = new ArrayList<>();
    for (SweepingOutputInstance instance : instances) {
      if (instanceIds.contains(instance.getUuid())) {
        continue;
      }

      instanceIds.add(instance.getUuid());
      newInstances.add(instance);
    }

    for (SweepingOutputInstance instance : newInstances) {
      sweepingOutputService.save(SweepingOutputServiceImpl
                                     .prepareSweepingOutputBuilder(appId, toPipelineExecutionId, toPipelineExecutionId,
                                         null, toStateExecutionId, SweepingOutputInstance.Scope.PIPELINE)
                                     .name(instance.getName())
                                     .output(instance.getOutput())
                                     .value(instance.getValue())
                                     .build());
    }
  }
}
