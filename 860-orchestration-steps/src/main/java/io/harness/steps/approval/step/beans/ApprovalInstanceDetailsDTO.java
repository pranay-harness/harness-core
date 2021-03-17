package io.harness.steps.approval.step.beans;

import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = HarnessApprovalInstanceDetailsDTO.class, name = "HarnessApproval") })
public interface ApprovalInstanceDetailsDTO {}
