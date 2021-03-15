package io.harness.pms.approval.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = HarnessApprovalInstanceDetailsDTO.class, name = "HarnessApproval") })
public interface ApprovalInstanceDetailsDTO {}
