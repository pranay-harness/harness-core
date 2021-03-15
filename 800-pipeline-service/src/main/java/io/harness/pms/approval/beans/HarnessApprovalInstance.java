package io.harness.pms.approval.beans;

import io.harness.plancreator.steps.approval.ApproverInputInfo;
import io.harness.plancreator.steps.approval.Approvers;
import io.harness.pms.approval.entities.ApprovalInstance;

import java.util.List;
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
@EqualsAndHashCode(callSuper = false)
@Entity(value = "approvalInstances", noClassnameStored = true)
@Persistent
@TypeAlias("HarnessApprovalInstances")
public class HarnessApprovalInstance extends ApprovalInstance {
  // List of ApprovalActivity per user.
  List<HarnessApprovalActivity> approvalActivities;
  Approvers approvers;
  List<ApproverInputInfo> approverInputs;
}
