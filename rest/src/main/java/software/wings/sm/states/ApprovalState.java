package software.wings.sm.states;

import static java.util.Arrays.asList;
import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class ApprovalState extends State {
  @Attributes(required = true, title = "Group Name") private String groupName;

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
    String approvalId = UUIDGenerator.getUuid();
    ApprovalStateExecutionData executionData = anApprovalStateExecutionData().withApprovalId(approvalId).build();
    return anExecutionResponse()
        .withAsync(true)
        .withExecutionStatus(ExecutionStatus.PAUSED)
        .withCorrelationIds(asList(approvalId))
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ApprovalStateExecutionData approvalNotifyResponse =
        (ApprovalStateExecutionData) response.values().iterator().next();

    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();

    executionData.setApprovedBy(approvalNotifyResponse.getApprovedBy());
    executionData.setComments(approvalNotifyResponse.getComments());
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

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    return Constants.DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
  }

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
