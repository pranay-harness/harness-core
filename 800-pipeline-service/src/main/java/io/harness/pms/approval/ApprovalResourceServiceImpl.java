package io.harness.pms.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.helpers.CurrentUserHelper;
import io.harness.steps.approval.step.ApprovalInstanceResponseMapper;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceAuthorizationDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ApprovalResourceServiceImpl implements ApprovalResourceService {
  private final ApprovalInstanceService approvalInstanceService;
  private final ApprovalInstanceResponseMapper approvalInstanceResponseMapper;
  private final CurrentUserHelper currentUserHelper;

  @Inject
  public ApprovalResourceServiceImpl(ApprovalInstanceService approvalInstanceService,
      ApprovalInstanceResponseMapper approvalInstanceResponseMapper, CurrentUserHelper currentUserHelper) {
    this.approvalInstanceService = approvalInstanceService;
    this.approvalInstanceResponseMapper = approvalInstanceResponseMapper;
    this.currentUserHelper = currentUserHelper;
  }

  @Override
  public ApprovalInstanceResponseDTO get(String approvalInstanceId) {
    return approvalInstanceResponseMapper.toApprovalInstanceResponseDTO(
        approvalInstanceService.get(approvalInstanceId));
  }

  @Override
  public ApprovalInstanceResponseDTO addHarnessApprovalActivity(
      @NotNull String approvalInstanceId, @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    if (!getHarnessApprovalInstanceAuthorization(approvalInstanceId).isAuthorized()) {
      throw new InvalidRequestException("User not authorized to approve/reject");
    }

    EmbeddedUser user = currentUserHelper.getFromSecurityContext();
    HarnessApprovalInstance instance =
        approvalInstanceService.addHarnessApprovalActivity(approvalInstanceId, user, request);
    return approvalInstanceResponseMapper.toApprovalInstanceResponseDTO(instance);
  }

  @Override
  public HarnessApprovalInstanceAuthorizationDTO getHarnessApprovalInstanceAuthorization(
      @NotNull String approvalInstanceId) {
    // TODO: Implement this method with 2 use cases:
    //   1. User not in approvers user group list should not be allowed
    //   2. User should not be allowed to approve multiple times
    return HarnessApprovalInstanceAuthorizationDTO.builder().authorized(true).build();
  }
}
