package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JiraTaskNgParametersBuilderConverter {
  public Function<CreateJiraTicketRequest, JiraTaskNGParametersBuilder> toJiraTaskNGParametersBuilderFromCreate =
      request
      -> JiraTaskNGParameters.builder()
             .project(request.getProject())
             .summary(request.getSummary())
             .description(request.getDescription())
             .issueType(request.getIssueType())
             .priority(request.getPriority())
             .labels(request.getLabels())
             .customFields(request.getCustomFields())
             .issueId(request.getIssueId())
             .updateIssueIds(request.getUpdateIssueIds())
             .status(request.getStatus())
             .comment(request.getComment())
             .createmetaExpandParam(request.getCreatemetaExpandParam())
             .activityId(request.getActivityId())
             .approvalId(request.getApprovalId())
             .approvalField(request.getApprovalField())
             .approvalValue(request.getApprovalValue())
             .rejectionField(request.getRejectionField())
             .rejectionValue(request.getRejectionValue());

  public Function<UpdateJiraTicketRequest, JiraTaskNGParametersBuilder> toJiraTaskNGParametersBuilderFromUpdate =
      request
      -> JiraTaskNGParameters.builder()
             .project(request.getProject())
             .summary(request.getSummary())
             .description(request.getDescription())
             .issueType(request.getIssueType())
             .priority(request.getPriority())
             .labels(request.getLabels())
             .customFields(request.getCustomFields())
             .updateIssueIds(request.getUpdateIssueIds())
             .status(request.getStatus())
             .comment(request.getComment())
             .createmetaExpandParam(request.getCreatemetaExpandParam())
             .activityId(request.getActivityId())
             .approvalId(request.getApprovalId())
             .approvalField(request.getApprovalField())
             .approvalValue(request.getApprovalValue())
             .rejectionField(request.getRejectionField())
             .rejectionValue(request.getRejectionValue());
}
