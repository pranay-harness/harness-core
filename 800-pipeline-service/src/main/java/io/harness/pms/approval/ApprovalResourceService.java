/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.approval;

import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceAuthorizationDTO;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface ApprovalResourceService {
  ApprovalInstanceResponseDTO get(@NotNull String approvalInstanceId);
  ApprovalInstanceResponseDTO addHarnessApprovalActivity(
      @NotNull String approvalInstanceId, @NotNull @Valid HarnessApprovalActivityRequestDTO request);
  HarnessApprovalInstanceAuthorizationDTO getHarnessApprovalInstanceAuthorization(@NotNull String approvalInstanceId);
}
