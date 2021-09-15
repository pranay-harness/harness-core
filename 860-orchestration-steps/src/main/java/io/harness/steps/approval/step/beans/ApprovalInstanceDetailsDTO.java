/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.jira.beans.JiraApprovalInstanceDetailsDTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HarnessApprovalInstanceDetailsDTO.class, name = StepSpecTypeConstants.HARNESS_APPROVAL)
  , @JsonSubTypes.Type(value = JiraApprovalInstanceDetailsDTO.class, name = StepSpecTypeConstants.JIRA_APPROVAL)
})
public interface ApprovalInstanceDetailsDTO {}
