package io.harness.pms.approval;

import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.helpers.CurrentUserHelper;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalAction;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceAuthorizationDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class ApprovalResourceServiceImpl implements ApprovalResourceService {
  private final ApprovalInstanceService approvalInstanceService;
  private final TransactionTemplate transactionTemplate;
  private final CurrentUserHelper currentUserHelper;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public ApprovalResourceServiceImpl(ApprovalInstanceService approvalInstanceService,
      TransactionTemplate transactionTemplate, CurrentUserHelper currentUserHelper) {
    this.approvalInstanceService = approvalInstanceService;
    this.transactionTemplate = transactionTemplate;
    this.currentUserHelper = currentUserHelper;
  }

  @Override
  public ApprovalInstanceResponseDTO get(String approvalInstanceId) {
    return ApprovalInstanceResponseDTO.fromApprovalInstance(approvalInstanceService.get(approvalInstanceId));
  }

  @Override
  public ApprovalInstanceResponseDTO addHarnessApprovalActivity(
      @NotNull String approvalInstanceId, @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    if (!getHarnessApprovalInstanceAuthorization(approvalInstanceId).isAuthorized()) {
      throw new InvalidRequestException("User not authorized to approve/reject");
    }

    EmbeddedUser user = currentUserHelper.getFromSecurityContext();
    return doTransaction(status -> addHarnessApprovalActivityCallback(approvalInstanceId, user, request));
  }

  private ApprovalInstanceResponseDTO addHarnessApprovalActivityCallback(@NotNull String approvalInstanceId,
      EmbeddedUser user, @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    HarnessApprovalInstance instance = fetchWaitingHarnessApproval(approvalInstanceId);
    ApprovalStatus status;
    if (System.currentTimeMillis() > instance.getDeadline()) {
      status = ApprovalStatus.EXPIRED;
      request = null;
    } else if (request.getAction() == HarnessApprovalAction.REJECT) {
      status = ApprovalStatus.REJECTED;
    } else {
      int newCount = (instance.getApprovalActivities() == null ? 0 : instance.getApprovalActivities().size()) + 1;
      status = (newCount >= (Integer) instance.getApprovers().getMinimumCount().fetchFinalValue())
          ? ApprovalStatus.APPROVED
          : ApprovalStatus.WAITING;
    }
    return ApprovalInstanceResponseDTO.fromApprovalInstance(
        approvalInstanceService.addHarnessApprovalActivity(approvalInstanceId, status, user, request));
  }

  @Override
  public HarnessApprovalInstanceAuthorizationDTO getHarnessApprovalInstanceAuthorization(
      @NotNull String approvalInstanceId) {
    // TODO: Implement this method
    return HarnessApprovalInstanceAuthorizationDTO.builder().authorized(true).build();
  }

  private HarnessApprovalInstance fetchWaitingHarnessApproval(String approvalInstanceId) {
    ApprovalInstance tmpInstance = approvalInstanceService.get(approvalInstanceId);
    if (tmpInstance == null || tmpInstance.getType() != ApprovalType.HARNESS_APPROVAL) {
      throw new InvalidRequestException(String.format("Invalid harness approval instance id: %s", approvalInstanceId));
    }

    HarnessApprovalInstance instance = (HarnessApprovalInstance) tmpInstance;
    if (instance.getStatus() == ApprovalStatus.EXPIRED) {
      throw new InvalidRequestException("Harness approval instance has already expired");
    }
    if (instance.getStatus() != ApprovalStatus.WAITING) {
      throw new InvalidRequestException(
          String.format("Harness approval instance already completed. Status: %s", instance.getStatus()));
    }
    return instance;
  }

  private <T> T doTransaction(TransactionCallback<T> callback) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(callback));
  }
}
