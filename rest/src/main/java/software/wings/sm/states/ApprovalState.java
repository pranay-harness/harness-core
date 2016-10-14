package software.wings.sm.states;

import org.mongodb.morphia.annotations.Transient;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import javax.inject.Inject;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class ApprovalState extends State {
  private String groupName;

  @Transient @Inject private PipelineService pipelineService;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public ApprovalState(String name) {
    super(name, StateType.APPROVAL.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    Misc.quietSleep(2000);
    pipelineService.refreshPipelineExecutionAsync(context.getApp().getUuid(), context.getWorkflowExecutionId());
    return new ExecutionResponse();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets group name.
   *
   * @return the group name
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets group name.
   *
   * @param groupName the group name
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }
}
