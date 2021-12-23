package io.harness.steps.approval.step.servicenow.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.servicenow.ServiceNowTicketKeyNG;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapperDTO;

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
@ApiModel("ServiceNowApprovalInstanceDetails")
@Schema(name = "ServiceNowApprovalInstanceDetails", description = "This contains details of Jira Approval Instance")
public class ServiceNowApprovalInstanceDetailsDTO implements ApprovalInstanceDetailsDTO {
  @NotEmpty String connectorRef;
  @NotNull ServiceNowTicketKeyNG ticket;
  @NotNull CriteriaSpecWrapperDTO approvalCriteria;
  @NotNull CriteriaSpecWrapperDTO rejectionCriteria;
}
