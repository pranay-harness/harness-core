package io.harness.steps.approval.step.harness.entities;

import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.HarnessApprovalOutcome;
import io.harness.steps.approval.step.harness.HarnessApprovalStepParameters;
import io.harness.steps.approval.step.harness.beans.ApproverInput;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfoDTO;
import io.harness.steps.approval.step.harness.beans.ApproversDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalAction;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivity;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
  @NotNull ApproversDTO approvers;
  List<ApproverInputInfoDTO> approverInputs;

  private Optional<HarnessApprovalActivity> fetchLastApprovalActivity() {
    if (EmptyPredicate.isEmpty(approvalActivities)) {
      return Optional.empty();
    }
    return Optional.of(approvalActivities.get(approvalActivities.size() - 1));
  }

  public void addApprovalActivity(EmbeddedUser user, HarnessApprovalActivityRequestDTO request) {
    if (request.getAction() == HarnessApprovalAction.APPROVE
        && !validateApprovalInputsRequest(request.getApproverInputs())) {
      throw new InvalidRequestException("Invalid approver inputs");
    }

    HarnessApprovalActivity approvalActivity =
        HarnessApprovalActivity.fromHarnessApprovalActivityRequestDTO(user, request);
    if (approvalActivity == null) {
      return;
    }

    if (EmptyPredicate.isEmpty(approvalActivities)) {
      approvalActivities = Collections.singletonList(approvalActivity);
    } else {
      approvalActivities = new ArrayList<>(approvalActivities);
      approvalActivities.add(approvalActivity);
    }
  }

  private boolean validateApprovalInputsRequest(List<ApproverInput> requestInputs) {
    if (EmptyPredicate.isEmpty(requestInputs) || EmptyPredicate.isEmpty(approverInputs)) {
      return EmptyPredicate.isEmpty(requestInputs) && EmptyPredicate.isEmpty(approverInputs);
    }
    if (requestInputs.size() != approverInputs.size()) {
      return false;
    }

    Set<String> names =
        approverInputs.stream().map(ApproverInputInfoDTO::getName).filter(Objects::nonNull).collect(Collectors.toSet());
    if (names.size() != approverInputs.size()) {
      return false;
    }

    for (ApproverInput requestInput : requestInputs) {
      if (requestInput != null && requestInput.getName() != null) {
        names.remove(requestInput.getName());
      }
    }
    return names.size() == 0;
  }

  @Override
  public ApprovalInstanceDetailsDTO toApprovalInstanceDetailsDTO() {
    return HarnessApprovalInstanceDetailsDTO.builder()
        .approvers(approvers)
        .approvalActivities(approvalActivities)
        .approverInputs(
            fetchLastApprovalActivity()
                .map(approvalActivity
                    -> approvalActivity.getApproverInputs() == null ? new ArrayList<ApproverInputInfoDTO>()
                                                                    : approvalActivity.getApproverInputs()
                                                                          .stream()
                                                                          .map(ApproverInput::toApproverInputInfoDTO)
                                                                          .collect(Collectors.toList()))
                .orElse(approverInputs))
        .build();
  }

  public HarnessApprovalOutcome toHarnessApprovalOutcome() {
    return HarnessApprovalOutcome.builder()
        .approvalActivities(approvalActivities)
        .approverInputs(getStatus() == ApprovalStatus.APPROVED
                ? fetchLastApprovalActivity().map(HarnessApprovalActivity::getApproverInputs).orElse(null)
                : null)
        .build();
  }

  public static HarnessApprovalInstance fromStepParameters(
      Ambiance ambiance, HarnessApprovalStepParameters stepParameters) {
    if (stepParameters == null) {
      return null;
    }

    HarnessApprovalInstance instance = HarnessApprovalInstance.builder()
                                           .approvalActivities(new ArrayList<>())
                                           .approvers(ApproversDTO.fromApprovers(stepParameters.getApprovers()))
                                           .approverInputs(stepParameters.getApproverInputs() == null
                                                   ? null
                                                   : stepParameters.getApproverInputs()
                                                         .stream()
                                                         .map(ApproverInputInfoDTO::fromApproverInputInfo)
                                                         .collect(Collectors.toList()))
                                           .build();
    instance.updateFromStepParameters(ambiance, stepParameters);
    return instance;
  }
}
