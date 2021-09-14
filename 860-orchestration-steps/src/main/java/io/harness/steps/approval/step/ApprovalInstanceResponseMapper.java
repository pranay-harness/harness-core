/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.jira.JiraIssueKeyNG;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.beans.ApproverInput;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfoDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;
import io.harness.steps.approval.step.jira.JiraApprovalHelperService;
import io.harness.steps.approval.step.jira.beans.JiraApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
public class ApprovalInstanceResponseMapper {
  private final JiraApprovalHelperService jiraApprovalHelperService;

  @Inject
  public ApprovalInstanceResponseMapper(JiraApprovalHelperService jiraApprovalHelperService) {
    this.jiraApprovalHelperService = jiraApprovalHelperService;
  }

  public ApprovalInstanceResponseDTO toApprovalInstanceResponseDTO(ApprovalInstance instance) {
    if (instance == null) {
      return null;
    }

    return ApprovalInstanceResponseDTO.builder()
        .id(instance.getId())
        .type(instance.getType())
        .status(instance.getStatus())
        .deadline(instance.getDeadline())
        .details(toApprovalInstanceDetailsDTO(instance))
        .createdAt(instance.getCreatedAt())
        .lastModifiedAt(instance.getLastModifiedAt())
        .build();
  }

  private ApprovalInstanceDetailsDTO toApprovalInstanceDetailsDTO(ApprovalInstance instance) {
    switch (instance.getType()) {
      case HARNESS_APPROVAL:
        return toHarnessApprovalInstanceDetailsDTO((HarnessApprovalInstance) instance);
      case JIRA_APPROVAL:
        return toJiraApprovalInstanceDetailsDTO((JiraApprovalInstance) instance);
      default:
        return null;
    }
  }

  private ApprovalInstanceDetailsDTO toHarnessApprovalInstanceDetailsDTO(HarnessApprovalInstance instance) {
    return HarnessApprovalInstanceDetailsDTO.builder()
        .approvalMessage(instance.getApprovalMessage())
        .includePipelineExecutionHistory(instance.isIncludePipelineExecutionHistory())
        .approvers(instance.getApprovers())
        .approvalActivities(instance.getApprovalActivities())
        .approverInputs(
            instance.fetchLastApprovalActivity()
                .map(approvalActivity
                    -> approvalActivity.getApproverInputs() == null ? new ArrayList<ApproverInputInfoDTO>()
                                                                    : approvalActivity.getApproverInputs()
                                                                          .stream()
                                                                          .map(ApproverInput::toApproverInputInfoDTO)
                                                                          .collect(Collectors.toList()))
                .orElse(instance.getApproverInputs()))
        .build();
  }

  private ApprovalInstanceDetailsDTO toJiraApprovalInstanceDetailsDTO(JiraApprovalInstance instance) {
    JiraConnectorDTO connectorDTO = jiraApprovalHelperService.getJiraConnector(
        AmbianceUtils.getAccountId(instance.getAmbiance()), AmbianceUtils.getOrgIdentifier(instance.getAmbiance()),
        AmbianceUtils.getProjectIdentifier(instance.getAmbiance()), instance.getConnectorRef());

    return JiraApprovalInstanceDetailsDTO.builder()
        .connectorRef(instance.getConnectorRef())
        .issue(new JiraIssueKeyNG(connectorDTO.getJiraUrl(), instance.getIssueKey()))
        .approvalCriteria(instance.getApprovalCriteria())
        .rejectionCriteria(instance.getRejectionCriteria())
        .build();
  }
}
