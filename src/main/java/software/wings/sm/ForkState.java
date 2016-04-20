/**
 *
 */
package software.wings.sm;

import software.wings.app.WingsBootstrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 */
public class ForkState extends State {
  private static final long serialVersionUID = 1L;

  private List<String> forkStateNames = new ArrayList<String>();

  public ForkState(String name) {
    super(name, StateType.FORK.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    SMInstance smInstance = context.getSmInstance();
    List<String> correlationIds = new ArrayList<>();
    for (String state : forkStateNames) {
      String notifyId = smInstance.getUuid() + "-forkTo-" + state;
      WingsBootstrap.lookup(StateMachineExecutor.class)
          .execute(smInstance.getStateMachineId(), state, context, smInstance.getUuid(), notifyId);
      correlationIds.add(notifyId);
    }

    ExecutionResponse executionResponse = new ExecutionResponse();
    executionResponse.setAsynch(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  @Override
  public ExecutionResponse handleAsynchResponse(
      ExecutionContext context, Map<String, ? extends Serializable> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    for (Serializable status : response.values()) {
      ExecutionStatus executionStatus = (ExecutionStatus) status;
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
      }
    }
    return executionResponse;
  }

  public List<String> getForkStateNames() {
    return forkStateNames;
  }

  public void setForkStateNames(List<String> forkStateNames) {
    this.forkStateNames = forkStateNames;
  }
}
