package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.delegate.beans.ResponseData;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ForkElement;
import software.wings.beans.LoopEnvStateParams;
import software.wings.beans.LoopEnvStateParams.LoopEnvStateParamsBuilder;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.ForkState.ForkStateExecutionData;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Slf4j
@FieldNameConstants(innerTypeName = "EnvLoopStateKeys")
public class EnvLoopState extends State {
  @EnumData(enumDataProvider = WorkflowServiceImpl.class) @Getter @Setter private String workflowId;

  @Getter @Setter private String pipelineId;
  @Getter @Setter private String pipelineStageElementId;
  @Getter @Setter private int pipelineStageParallelIndex;
  @Getter @Setter private String stageName;
  @Getter @Setter private String loopedVarName;
  @Getter @Setter private List<String> loopedValues;

  @JsonIgnore private Map<String, String> workflowVariables;

  @Getter @Setter @JsonIgnore private boolean disable;

  @Getter @Setter private String disableAssertion;
  @Transient @Inject private WorkflowService workflowService;

  public EnvLoopState(String name) {
    super(name, StateType.ENV_LOOP_STATE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return executeInternal((ExecutionContextImpl) context);
  }

  private ExecutionResponse executeInternal(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    ForkStateExecutionData forkStateExecutionData = new ForkStateExecutionData();
    List<String> forkStateNames = new ArrayList<>();
    forkStateExecutionData.setElements(new ArrayList<>());
    int i = 1;
    for (String loopedValue : loopedValues) {
      String state = getName() + "_" + i;
      forkStateNames.add(state);
      ForkElement element = ForkElement.builder().stateName(state).parentId(stateExecutionInstance.getUuid()).build();
      StateExecutionInstance childStateExecutionInstance = stateExecutionInstance.cloneInternal();
      childStateExecutionInstance.setStateParams(null);

      childStateExecutionInstance.setContextElement(element);
      childStateExecutionInstance.setDisplayName(state);
      childStateExecutionInstance.setStateName(state);
      childStateExecutionInstance.setParentLoopedState(true);
      childStateExecutionInstance.setLoopedStateParams(getLoopStateParams(loopedValue, state));
      childStateExecutionInstance.setStateType(StateType.ENV_STATE.getName());
      childStateExecutionInstance.setNotifyId(element.getUuid());
      executionResponseBuilder.stateExecutionInstance(childStateExecutionInstance);
      correlationIds.add(element.getUuid());
      forkStateExecutionData.getElements().add(childStateExecutionInstance.getContextElement().getName());
      i++;
    }
    forkStateExecutionData.setForkStateNames(forkStateNames);

    return executionResponseBuilder.stateExecutionData(forkStateExecutionData)
        .async(true)
        .correlationIds(correlationIds)
        .build();
  }

  private LoopEnvStateParams getLoopStateParams(String loopedValue, String name) {
    LoopEnvStateParamsBuilder loopEnvStateParams = LoopEnvStateParams.builder()
                                                       .pipelineId(pipelineId)
                                                       .pipelineStageElementId(pipelineStageElementId)
                                                       .pipelineStageParallelIndex(pipelineStageParallelIndex)
                                                       .stageName(stageName)
                                                       .disable(disable)
                                                       .disableAssertion(disableAssertion)
                                                       .stepName(name)
                                                       .workflowId(workflowId);

    if (workflowVariables != null) {
      Map<String, String> newWV = new HashMap<>(workflowVariables);
      newWV.put(loopedVarName, loopedValue);
      loopEnvStateParams.workflowVariables(newWV);
    }
    return loopEnvStateParams.build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    for (ResponseData status : response.values()) {
      ExecutionStatus executionStatus = ((ExecutionStatusResponseData) status).getExecutionStatus();
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponseBuilder.executionStatus(executionStatus);
      }
    }
    return executionResponseBuilder.build();
  }

  @Override
  public void parseProperties(Map<String, Object> properties) {
    boolean isDisabled =
        properties.get(EnvLoopStateKeys.disable) != null && (boolean) properties.get(EnvLoopStateKeys.disable);
    if (isDisabled && properties.get(EnvLoopStateKeys.disableAssertion) == null) {
      properties.put(EnvLoopStateKeys.disableAssertion, "true");
    }
    super.parseProperties(properties);
  }

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return EnvState.ENV_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
  }

  public void setWorkflowVariables(Map<String, String> workflowVariables) {
    this.workflowVariables = workflowVariables;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // nothing to handle
  }
}
