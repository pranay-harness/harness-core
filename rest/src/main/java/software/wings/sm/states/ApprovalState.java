package software.wings.sm.states;

import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.SortOrder;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.UserService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.util.List;
import javax.inject.Inject;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class ApprovalState extends State {
  private String groupName;

  @Transient @Inject private PipelineService pipelineService;

  @Transient @Inject private UserService userService;

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
    User user = null;
    List<User> response = userService
                              .list(PageRequest.Builder.aPageRequest()
                                        .addOrder("createdAt", SortOrder.OrderType.ASC)
                                        .withLimit("1")
                                        .build())
                              .getResponse();
    if (response != null && response.size() == 1) {
      user = response.get(0).getPublicUser(); // TODO::remove and fetch actual approver
    }

    ApprovalStateExecutionData executionData = anApprovalStateExecutionData()
                                                   .withApprovedBy(user)
                                                   .withComments("Auto approved.")
                                                   .withApprovedOn(System.currentTimeMillis())
                                                   .build();

    Misc.quietSleep(2000);
    pipelineService.refreshPipelineExecutionAsync(
        ((ExecutionContextImpl) context).getApp().getUuid(), context.getWorkflowExecutionId());
    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .build();
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
