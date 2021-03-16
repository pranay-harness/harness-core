package io.harness.steps.approval.step.harness.entities;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.HarnessApprovalStepParameters;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfoDTO;
import io.harness.steps.approval.step.harness.beans.Approvers;
import io.harness.steps.approval.step.harness.beans.ApproversDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivity;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "HarnessApprovalInstanceKeys")
@EqualsAndHashCode(callSuper = true)
@Entity(value = "approvalInstances", noClassnameStored = true)
@Persistent
@TypeAlias("harnessApprovalInstance")
public class HarnessApprovalInstance extends ApprovalInstance {
  @NotNull List<HarnessApprovalActivity> approvalActivities;
  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;

  public HarnessApprovalInstance fromStepParameters(Ambiance ambiance, HarnessApprovalStepParameters stepParameters) {
    if (stepParameters == null) {
      return null;
    }

    HarnessApprovalInstance instance = HarnessApprovalInstance.builder()
                                           .approvalActivities(new ArrayList<>())
                                           .approvers(stepParameters.getApprovers())
                                           .approverInputs(stepParameters.getApproverInputs())
                                           .build();
    instance.updateFromStepParameters(ambiance, stepParameters);
    return instance;
  }

  @Override
  public ApprovalInstanceDetailsDTO toApprovalInstanceDetailsDTO() {
    return HarnessApprovalInstanceDetailsDTO.builder()
        .approvers(ApproversDTO.fromApprovers(approvers))
        .approvalActivities(approvalActivities)
        .approverInputs(approverInputs == null
                ? null
                : approverInputs.stream().map(ApproverInputInfoDTO::fromApproverInputInfo).collect(Collectors.toList()))
        .build();
  }
}
