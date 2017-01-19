package software.wings.sm.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatus.ExecutionStatusData;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 12/16/16.
 */
public class SubWorkflowState extends State {
  private static final Logger logger = LoggerFactory.getLogger(SubWorkflowState.class);

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
    StateExecutionInstance childStateExecutionInstance =
        JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);

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
    ExecutionStatus executionStatus = ((ExecutionStatusData) response.values().iterator().next()).getExecutionStatus();
    if (executionStatus != ExecutionStatus.SUCCESS) {
      executionResponse.setExecutionStatus(executionStatus);
    }
    logger.info("Subworkflow state execution completed - stateExecutionInstanceId:{}, stateName:{}, executionStatus:{}",
        ((ExecutionContextImpl) context).getStateExecutionInstance().getUuid(), getName(),
        executionResponse.getExecutionStatus());

    return executionResponse;
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
