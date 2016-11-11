package software.wings.sm.states;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.SortOrder;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.UserService;
import software.wings.sm.*;
import software.wings.utils.Misc;

import javax.inject.Inject;

import java.util.List;

import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

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
    List<User> response =
        userService.list(PageRequest.Builder.aPageRequest().addOrder("createdAt", SortOrder.OrderType.ASC).build())
            .getResponse();
    User user = response.get(0).getPublicUser(); // TODO::remove and fetch actual approver

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
