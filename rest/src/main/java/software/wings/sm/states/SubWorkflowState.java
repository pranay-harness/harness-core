package software.wings.sm.states;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.KryoUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 12/16/16.
 */
public class SubWorkflowState extends State {
  private static final Logger logger = LoggerFactory.getLogger(SubWorkflowState.class);

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

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

    StateExecutionInstance childStateExecutionInstance = getSpawningInstance(stateExecutionInstance);
    executionResponse.add(childStateExecutionInstance);
    correlationIds.add(stateExecutionInstance.getUuid());

    executionResponse.setAsync(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  protected StateExecutionInstance getSpawningInstance(StateExecutionInstance stateExecutionInstance) {
    StateExecutionInstance childStateExecutionInstance = KryoUtils.clone(stateExecutionInstance);

    childStateExecutionInstance.setChildStateMachineId(subWorkflowId);
    childStateExecutionInstance.setStateName(null);
    childStateExecutionInstance.setStateType(null);
    childStateExecutionInstance.setNotifyId(stateExecutionInstance.getUuid());
    childStateExecutionInstance.setPrevInstanceId(null);
    childStateExecutionInstance.setContextTransition(true);
    childStateExecutionInstance.setStatus(ExecutionStatus.NEW);
    childStateExecutionInstance.setStartTs(null);
    childStateExecutionInstance.setEndTs(null);
    return childStateExecutionInstance;
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
   */
  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    handleStatusSummary(context, response, executionResponse);
    return executionResponse;
  }

  protected void handleStatusSummary(
      ExecutionContext context, Map<String, NotifyResponseData> response, ExecutionResponse executionResponse) {
    ExecutionStatus executionStatus = ((ExecutionStatusData) response.values().iterator().next()).getExecutionStatus();
    if (executionStatus != ExecutionStatus.SUCCESS) {
      executionResponse.setExecutionStatus(executionStatus);
    }
    logger.info("Subworkflow state execution completed - stateExecutionInstanceId:{}, stateName:{}, executionStatus:{}",
        ((ExecutionContextImpl) context).getStateExecutionInstance().getUuid(), getName(),
        executionResponse.getExecutionStatus());

    ElementStateExecutionData stateExecutionData = (ElementStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setElementStatusSummary(workflowExecutionService.getElementsSummary(
        context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionInstanceId()));
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getSubWorkflowId() {
    return subWorkflowId;
  }

  public void setSubWorkflowId(String subWorkflowId) {
    this.subWorkflowId = subWorkflowId;
  }
}
