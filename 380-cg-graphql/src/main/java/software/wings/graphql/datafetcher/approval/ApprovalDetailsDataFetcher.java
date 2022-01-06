/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.JIRA;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.SERVICENOW;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.SHELL_SCRIPT;
import static software.wings.sm.states.ApprovalState.ApprovalStateType.USER_GROUP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLApprovalDetailsInput;
import software.wings.graphql.schema.type.approval.QLApprovalDetails;
import software.wings.graphql.schema.type.approval.QLApprovalDetailsPayload;
import software.wings.graphql.schema.type.approval.QLJIRAApprovalDetails;
import software.wings.graphql.schema.type.approval.QLSNOWApprovalDetails;
import software.wings.graphql.schema.type.approval.QLShellScriptDetails;
import software.wings.graphql.schema.type.approval.QLUGApprovalDetails;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)
public class ApprovalDetailsDataFetcher
    extends AbstractObjectDataFetcher<QLApprovalDetailsPayload, QLApprovalDetailsInput> {
  @Inject protected WingsPersistence persistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private UserGroupService userGroupService;

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  public QLApprovalDetailsPayload fetch(QLApprovalDetailsInput qlApprovalDetailsInput, String accountId) {
    WorkflowExecution execution = persistence.createAuthorizedQuery(WorkflowExecution.class)
                                      .filter("_id", qlApprovalDetailsInput.getExecutionId())
                                      .get();
    if (execution == null) {
      throw new InvalidRequestException("Execution does not exist or access is denied", WingsException.USER);
    }

    List<QLUGApprovalDetails> ugApprovalDetailsList = new LinkedList<>();
    List<QLSNOWApprovalDetails> snowApprovalDetailsList = new LinkedList<>();
    List<QLJIRAApprovalDetails> jiraApprovalDetailsList = new LinkedList<>();
    List<QLShellScriptDetails> shellScriptApprovalDetailsList = new LinkedList<>();
    List<QLApprovalDetails> approvalDetails = new LinkedList<>();

    List<ApprovalStateExecutionData> approvalStateExecutionDataList =
        workflowExecutionService.fetchApprovalStateExecutionsDataFromWorkflowExecution(
            qlApprovalDetailsInput.getApplicationId(), qlApprovalDetailsInput.getExecutionId());

    approvalStateExecutionDataList.forEach(approvalStateExecutionData -> {
      transformApprovalDetails(approvalStateExecutionData, ugApprovalDetailsList, snowApprovalDetailsList,
          jiraApprovalDetailsList, shellScriptApprovalDetailsList);
    });

    approvalDetails.addAll(ugApprovalDetailsList);
    approvalDetails.addAll(snowApprovalDetailsList);
    approvalDetails.addAll(jiraApprovalDetailsList);
    approvalDetails.addAll(shellScriptApprovalDetailsList);

    return QLApprovalDetailsPayload.builder().approvalDetails(approvalDetails).build();
  }

  private void transformApprovalDetails(ApprovalStateExecutionData approvalStateExecutionData,
      List<QLUGApprovalDetails> ugApprovalDetailsList, List<QLSNOWApprovalDetails> snowApprovalDetailsList,
      List<QLJIRAApprovalDetails> jiraApprovalDetailsList, List<QLShellScriptDetails> shellScriptApprovalDetailsList) {
    if (USER_GROUP.equals(approvalStateExecutionData.getApprovalStateType())) {
      ugApprovalDetailsList.add(getUGApprovalDetails(approvalStateExecutionData));
    } else if (JIRA.equals(approvalStateExecutionData.getApprovalStateType())) {
      jiraApprovalDetailsList.add(getJIRAApprovalDetails(approvalStateExecutionData));
    } else if (SERVICENOW.equals(approvalStateExecutionData.getApprovalStateType())) {
      snowApprovalDetailsList.add(getSNOWApprovalDetails(approvalStateExecutionData));
    } else if (SHELL_SCRIPT.equals(approvalStateExecutionData.getApprovalStateType())) {
      shellScriptApprovalDetailsList.add(getShellScriptDetails(approvalStateExecutionData));
    }
  }

  private QLShellScriptDetails getShellScriptDetails(ApprovalStateExecutionData approvalStateExecutionData) {
    return QLShellScriptDetails.builder()
        .approvalId(approvalStateExecutionData.getApprovalId())
        .approvalName(approvalStateExecutionData.getStateName())
        .approvalType(approvalStateExecutionData.getApprovalStateType())
        .stepName(approvalStateExecutionData.getStateName())
        .stageName(approvalStateExecutionData.getStageName())
        .retryInterval((long) approvalStateExecutionData.getRetryInterval())
        .startedAt(approvalStateExecutionData.getStartTs())
        .willExpireAt(approvalStateExecutionData.getStartTs() + approvalStateExecutionData.getTimeoutMillis())
        .triggeredBy(approvalStateExecutionData.getTriggeredBy())
        .build();
  }

  private QLSNOWApprovalDetails getSNOWApprovalDetails(ApprovalStateExecutionData approvalStateExecutionData) {
    return QLSNOWApprovalDetails.builder()
        .approvalId(approvalStateExecutionData.getApprovalId())
        .approvalName(approvalStateExecutionData.getStateName())
        .approvalType(approvalStateExecutionData.getApprovalStateType())
        .stepName(approvalStateExecutionData.getStateName())
        .stageName(approvalStateExecutionData.getStageName())
        .ticketUrl(approvalStateExecutionData.getTicketUrl())
        .approvalCondition(approvalStateExecutionData.getSnowApproval().conditionsString(" "))
        .rejectionCondition(approvalStateExecutionData.getSnowRejection().conditionsString(" "))
        .startedAt(approvalStateExecutionData.getStartTs())
        .willExpireAt(approvalStateExecutionData.getStartTs() + approvalStateExecutionData.getTimeoutMillis())
        .ticketType(approvalStateExecutionData.getTicketType())
        .currentStatus(approvalStateExecutionData.getApprovalCurrentStatus().replaceAll("\n", " "))
        .triggeredBy(approvalStateExecutionData.getTriggeredBy())
        .build();
  }

  private QLJIRAApprovalDetails getJIRAApprovalDetails(ApprovalStateExecutionData approvalStateExecutionData) {
    return QLJIRAApprovalDetails.builder()
        .approvalId(approvalStateExecutionData.getApprovalId())
        .approvalName(approvalStateExecutionData.getStateName())
        .approvalType(approvalStateExecutionData.getApprovalStateType())
        .stepName(approvalStateExecutionData.getStateName())
        .stageName(approvalStateExecutionData.getStageName())
        .issueUrl(approvalStateExecutionData.getIssueUrl())
        .issueKey(approvalStateExecutionData.getIssueKey())
        .currentStatus(approvalStateExecutionData.getApprovalCurrentStatus())
        .approvalCondition(approvalStateExecutionData.getJiraApprovalCriteria())
        .rejectionCondition(approvalStateExecutionData.getJiraRejectionCriteria())
        .startedAt(approvalStateExecutionData.getStartTs())
        .willExpireAt(approvalStateExecutionData.getStartTs() + approvalStateExecutionData.getTimeoutMillis())
        .triggeredBy(approvalStateExecutionData.getTriggeredBy())
        .build();
  }

  private QLUGApprovalDetails getUGApprovalDetails(ApprovalStateExecutionData approvalStateExecutionData) {
    return QLUGApprovalDetails.builder()
        .approvalId(approvalStateExecutionData.getApprovalId())
        .appId(approvalStateExecutionData.getAppId())
        .approvalName(approvalStateExecutionData.getStateName())
        .approvalType(approvalStateExecutionData.getApprovalStateType())
        .stepName(approvalStateExecutionData.getStateName())
        .stageName(approvalStateExecutionData.getStageName())
        .timeoutMillis(approvalStateExecutionData.getTimeoutMillis())
        .triggeredBy(approvalStateExecutionData.getTriggeredBy())
        .approvers(getUserGroupNamesList(approvalStateExecutionData.getUserGroups()))
        .startedAt(approvalStateExecutionData.getStartTs())
        .willExpireAt(approvalStateExecutionData.getStartTs() + approvalStateExecutionData.getTimeoutMillis())
        .variables(approvalStateExecutionData.getVariables())
        .executionId(approvalStateExecutionData.getExecutionUuid())
        .build();
  }

  private List<String> getUserGroupNamesList(List<String> approvers) {
    return userGroupService.fetchUserGroupNamesFromIds(approvers)
        .stream()
        .map(UserGroup::getName)
        .collect(Collectors.toList());
  }
}
