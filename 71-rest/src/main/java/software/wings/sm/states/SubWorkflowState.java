package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.NameValuePair;
import software.wings.common.Constants;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.ExecutionStatusResponseData;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 12/16/16.
 */
@Slf4j
public class SubWorkflowState extends State {
  private List<NameValuePair> variableOverrides;

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;

  /**
   * Instantiates a new repeat state.
   *
   * @param name the name
   */
  public SubWorkflowState(String name) {
    super(name, StateType.SUB_WORKFLOW.name());
  }

  protected SubWorkflowState(String name, String stateType) {
    super(name, stateType);
  }

  private String subWorkflowId;

  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    ExecutionContextImpl context = (ExecutionContextImpl) contextIntf;
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    StateExecutionInstance childStateExecutionInstance = getSpawningInstance(stateExecutionInstance);
    correlationIds.add(stateExecutionInstance.getUuid());

    return ExecutionResponse.builder()
        .stateExecutionInstance(childStateExecutionInstance)
        .async(true)
        .correlationIds(correlationIds)
        .build();
  }

  protected StateExecutionInstance getSpawningInstance(StateExecutionInstance stateExecutionInstance) {
    StateExecutionInstance childStateExecutionInstance = KryoUtils.clone(stateExecutionInstance);
    childStateExecutionInstance.setStateParams(null);

    childStateExecutionInstance.setChildStateMachineId(subWorkflowId);
    childStateExecutionInstance.setDisplayName(null);
    childStateExecutionInstance.setStateName(null);
    childStateExecutionInstance.setStateType(null);
    childStateExecutionInstance.setNotifyId(stateExecutionInstance.getUuid());
    childStateExecutionInstance.setPrevInstanceId(null);
    childStateExecutionInstance.setDelegateTaskId(null);
    childStateExecutionInstance.setContextTransition(true);
    childStateExecutionInstance.setStatus(ExecutionStatus.NEW);
    childStateExecutionInstance.setContextElement(null);
    childStateExecutionInstance.setStartTs(null);
    childStateExecutionInstance.setEndTs(null);
    childStateExecutionInstance.setCreatedAt(0);
    childStateExecutionInstance.setLastUpdatedAt(0);
    if (childStateExecutionInstance.getStateExecutionMap() != null) {
      childStateExecutionInstance.getStateExecutionMap().clear();
    }
    return childStateExecutionInstance;
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
   */
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    handleStatusSummary(workflowExecutionService, context, response, executionResponseBuilder);
    return executionResponseBuilder.build();
  }

  protected void handleStatusSummary(WorkflowExecutionService workflowExecutionService, ExecutionContext context,
      Map<String, ResponseData> response, ExecutionResponseBuilder executionResponseBuilder) {
    ExecutionStatus executionStatus =
        ((ExecutionStatusResponseData) response.values().iterator().next()).getExecutionStatus();
    if (executionStatus != ExecutionStatus.SUCCESS) {
      executionResponseBuilder.executionStatus(executionStatus);
    }
    logger.info("Subworkflow state execution completed - stateExecutionInstanceId:{}, displayName:{}",
        ((ExecutionContextImpl) context).getStateExecutionInstance().getUuid(), getName());

    ElementStateExecutionData stateExecutionData = (ElementStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setElementStatusSummary(workflowExecutionService.getElementsSummary(
        context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionInstanceId()));
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return Constants.DEFAULT_PARENT_STATE_TIMEOUT_MILLIS;
  }

  public String getSubWorkflowId() {
    return subWorkflowId;
  }

  public void setSubWorkflowId(String subWorkflowId) {
    this.subWorkflowId = subWorkflowId;
  }

  public List<NameValuePair> getVariableOverrides() {
    return variableOverrides;
  }

  public void setVariableOverrides(List<NameValuePair> variableOverrides) {
    this.variableOverrides = variableOverrides;
  }
}
