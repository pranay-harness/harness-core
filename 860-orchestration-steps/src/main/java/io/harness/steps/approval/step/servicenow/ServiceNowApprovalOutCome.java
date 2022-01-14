package io.harness.steps.approval.step.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("serviceNowApprovalOutcome")
@TypeAlias("serviceNowApprovalOutcome")
@RecasterAlias("io.harness.steps.approval.step.servicenow.ServiceNowApprovalOutcome")
public class ServiceNowApprovalOutCome implements Outcome {}
