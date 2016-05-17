package software.wings.sm.states;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.WingsBootstrap;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.State;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Describes a ForkState by which we can fork execution to multiple threads in state machine.
 *
 * @author Rishi
 */
public class ForkState extends State {
  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private List<String> forkStateNames = new ArrayList<String>();

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
    StateExecutionInstance stateExecutionInstance = context.getSmInstance();
    List<String> correlationIds = new ArrayList<>();
    for (String state : forkStateNames) {
      String notifyId = stateExecutionInstance.getUuid() + "-forkTo-" + state;
      ExecutionContextImpl contextClone = SerializationUtils.clone(context);
      WingsBootstrap.lookup(StateMachineExecutor.class)
          .execute(stateExecutionInstance.getStateMachineId(), state, contextClone, stateExecutionInstance.getUuid(),
              notifyId);
      correlationIds.add(notifyId);
    }

    ExecutionResponse executionResponse = new ExecutionResponse();
    executionResponse.setAsynch(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

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
    logger.info("Fork state execution completed - smInstanceId:{}, stateName:{}, executionStatus:{}",
        context.getSmInstance().getUuid(), getName(), executionResponse.getExecutionStatus());
    return executionResponse;
  }

  public List<String> getForkStateNames() {
    return forkStateNames;
  }

  public void setForkStateNames(List<String> forkStateNames) {
    this.forkStateNames = forkStateNames;
  }

  public void addForkState(State state) {
    this.forkStateNames.add(state.getName());
  }
}
