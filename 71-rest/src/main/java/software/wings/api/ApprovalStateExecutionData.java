package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.NameValuePair;
import software.wings.beans.security.UserGroup;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ApprovalStateExecutionDataKeys")
public class ApprovalStateExecutionData extends StateExecutionData implements DelegateTaskNotifyResponseData {
  public static final String USER_GROUP_NAMES = "userGroupNames";
  public static final String USER_GROUPS_DISPLAY_NAME = "Approval User Groups";
  public static final String AUTHORIZATION_STATUS = "authorizationStatus";
  public static final String AUTHORIZATION_STATUS_DISPLAY_NAME = "Authorization Status";
  public static final String AUTHORIZATION_STATUS_VALUE = "User is not authorized to approve or reject";
  public static final String IS_USER_AUTHORIZED = "isUserAuthorized";
  public static final String AUTHORIZED_DISPLAY_NAME = "Authorized";

  private EmbeddedUser approvedBy;
  private Long approvedOn;
  private String comments;
  private String approvalId;
  private String workflowId;
  private String appId;
  private Integer timeoutMillis;

  private ApprovalStateType approvalStateType;

  /** User group Approval */
  private List<String> userGroups;

  /** Jira Approval */
  private String issueUrl;
  private String issueKey;
  private String currentStatus;
  private String approvalField;
  private String approvalValue;
  private String rejectionField;
  private String rejectionValue;

  /** ServiceNow Approval */
  private String ticketUrl;
  private ServiceNowTicketType ticketType;

  /** Slack approval */
  private boolean approvalFromSlack;

  // Shell script approval
  private String activityId;

  // Setting these variables for pipeline executions with only approval state
  @Transient private transient List<UserGroup> userGroupList;
  @Transient private transient boolean isAuthorized;

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Transient @Inject private transient UserGroupService userGroupService;

  private String skipAssertionResponse;
  private List<NameValuePair> variables;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "approvalStateType",
        ExecutionDataValue.builder().displayName("approvalStateType").value(approvalStateType).build());
    return setExecutionData(executionDetails);
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    populateApprovalStateAuthorizationData(executionDetails);
    return setExecutionData(executionDetails);
  }
  private Map<String, ExecutionDataValue> setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
    putNotNull(executionDetails, "approvalId",
        ExecutionDataValue.builder().displayName("Approval Id").value(approvalId).build());
    putNotNull(executionDetails, "status",
        ExecutionDataValue.builder().displayName("Approval Status").value(getStatus()).build());
    putNotNull(
        executionDetails, "issueUrl", ExecutionDataValue.builder().displayName("Issue URL").value(issueUrl).build());
    if (ticketUrl != null && ticketType != null) {
      putNotNull(executionDetails, "ticketUrl",
          ExecutionDataValue.builder().displayName(ticketType.getDisplayName() + " URL").value(ticketUrl).build());
    }

    if (userGroupService != null) {
      userGroupList = userGroupService.fetchUserGroupNamesFromIds(userGroups);
    }

    if (isNotEmpty(userGroupList)) {
      putNotNull(executionDetails, USER_GROUP_NAMES,
          ExecutionDataValue.builder().displayName(USER_GROUPS_DISPLAY_NAME).value(userGroupList).build());
    }

    if (timeoutMillis != null) {
      Integer timeoutMins = timeoutMillis / (60 * 1000);
      putNotNull(executionDetails, "timeoutMins",
          ExecutionDataValue.builder().displayName("Timeout").value(timeoutMins + " minutes").build());
    }

    if (EmptyPredicate.isNotEmpty(approvalField) && EmptyPredicate.isNotEmpty(approvalValue)) {
      putNotNull(executionDetails, "approvalCriteria",
          ExecutionDataValue.builder()
              .displayName("Approval Criteria")
              .value(StringUtils.capitalize(approvalField) + " : " + StringUtils.capitalize(approvalValue))
              .build());
    }

    if (EmptyPredicate.isNotEmpty(currentStatus)) {
      putNotNull(executionDetails, "currentStatus",
          ExecutionDataValue.builder()
              .displayName("Current value")
              .value(StringUtils.capitalize(approvalField) + " : " + StringUtils.capitalize(currentStatus))
              .build());
    }

    if (EmptyPredicate.isNotEmpty(rejectionField) && EmptyPredicate.isNotEmpty(rejectionValue)) {
      putNotNull(executionDetails, "rejectionCriteria",
          ExecutionDataValue.builder()
              .displayName("Rejection Criteria")
              .value(StringUtils.capitalize(rejectionField) + " : " + StringUtils.capitalize(rejectionValue))
              .build());
    }

    if (approvedBy != null) {
      if (getStatus() == ExecutionStatus.SUCCESS) {
        putNotNull(executionDetails, "approvedBy",
            ExecutionDataValue.builder().displayName("Approved By").value(approvedBy).build());
      } else if (getStatus() == ExecutionStatus.REJECTED) {
        putNotNull(executionDetails, "approvedBy",
            ExecutionDataValue.builder().displayName("Rejected By").value(approvedBy).build());
      }
    }

    putNotNull(executionDetails, "approvedOn",
        ExecutionDataValue.builder().displayName("Approved On").value(approvedOn).build());
    putNotNull(executionDetails, ApprovalStateExecutionDataKeys.variables,
        ExecutionDataValue.builder().displayName(ApprovalStateExecutionDataKeys.variables).value(variables).build());
    if (isNotEmpty(comments)) {
      executionDetails.put("comments", ExecutionDataValue.builder().displayName("Comments").value(comments).build());
    }

    return executionDetails;
  }

  private void populateApprovalStateAuthorizationData(Map<String, ExecutionDataValue> executionDetails) {
    if (workflowExecutionService != null) {
      isAuthorized = workflowExecutionService.verifyAuthorizedToAcceptOrReject(userGroups, asList(appId), workflowId);
    }

    if (isAuthorized) {
      putNotNull(executionDetails, IS_USER_AUTHORIZED,
          ExecutionDataValue.builder().displayName(AUTHORIZED_DISPLAY_NAME).value(true).build());
    } else {
      putNotNull(executionDetails, AUTHORIZATION_STATUS,
          ExecutionDataValue.builder()
              .displayName(AUTHORIZATION_STATUS_DISPLAY_NAME)
              .value(AUTHORIZATION_STATUS_VALUE)
              .build());
      putNotNull(executionDetails, IS_USER_AUTHORIZED,
          ExecutionDataValue.builder().displayName(AUTHORIZED_DISPLAY_NAME).value(false).build());
    }
  }
}
