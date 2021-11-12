package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jira.JiraIssueKeyNG;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("JiraApprovalInstanceDetails")
@Schema(name = "JiraApprovalInstanceDetails", description = "This contains details of Jira Approval Instance")
public class JiraApprovalInstanceDetailsDTO implements ApprovalInstanceDetailsDTO {
  @NotEmpty String connectorRef;
  @NotNull JiraIssueKeyNG issue;
  @NotNull CriteriaSpecWrapperDTO approvalCriteria;
  @NotNull CriteriaSpecWrapperDTO rejectionCriteria;
}
