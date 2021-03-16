package io.harness.pms.approval.entities;

import io.harness.pms.approval.beans.HarnessApprovalActivity;
import io.harness.steps.approval.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.harness.beans.Approvers;

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
@TypeAlias("harnessApprovalInstance")
public class HarnessApprovalInstance extends ApprovalInstance {
  // List of ApprovalActivity per user.
  List<HarnessApprovalActivity> approvalActivities;
  Approvers approvers;
  List<ApproverInputInfo> approverInputs;
}
