package software.wings.sm.states;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Describes a ForkState by which we can fork execution to multiple threads in state machine.
 *
 * @author Rishi
 */
public class ForkState extends State {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private List<String> forkStateNames = new ArrayList<>();

  /**
   * Instantiates a new fork state.
   *
   * @param name the name
   */
  public ForkState(String name) {
    super(name, StateType.FORK.name());
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    ExecutionContextImpl context = (ExecutionContextImpl) contextIntf;
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();
    for (String state : forkStateNames) {
      String notifyId = stateExecutionInstance.getUuid() + "-forkTo-" + state;
      StateExecutionInstance childStateExecutionInstance =
          JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);

      childStateExecutionInstance.setStateName(state);
      childStateExecutionInstance.setNotifyId(notifyId);
      executionResponse.add(childStateExecutionInstance);
      correlationIds.add(notifyId);
    }

    executionResponse.setAsynch(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#handleAsynchResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
   */
  @Override
  public ExecutionResponse handleAsynchResponse(
      ExecutionContextImpl context, Map<String, ? extends Serializable> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    for (Serializable status : response.values()) {
      ExecutionStatus executionStatus = (ExecutionStatus) status;
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      }
    }
    logger.info("Fork state execution completed - stateExecutionInstanceId:{}, stateName:{}, executionStatus:{}",
        context.getStateExecutionInstance().getUuid(), getName(), executionResponse.getExecutionStatus());
    return executionResponse;
  }

  public List<String> getForkStateNames() {
    return forkStateNames;
  }

  public void setForkStateNames(List<String> forkStateNames) {
    this.forkStateNames = forkStateNames;
  }

  /**
   * Adds the fork state.
   *
   * @param state the state
   */
  public void addForkState(State state) {
    this.forkStateNames.add(state.getName());
  }
}
