package io.harness.steps.approval.step;

import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ApprovalInstanceRepository;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.entities.ApprovalInstance.ApprovalInstanceKeys;
import io.harness.steps.approval.step.harness.HarnessApprovalResponseData;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalAction;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;
import io.harness.utils.RetryUtils;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.mongodb.morphia.mapping.Mapper;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class ApprovalInstanceServiceImpl implements ApprovalInstanceService {
  private final ApprovalInstanceRepository approvalInstanceRepository;
  private final TransactionTemplate transactionTemplate;
  private final WaitNotifyEngine waitNotifyEngine;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public ApprovalInstanceServiceImpl(ApprovalInstanceRepository approvalInstanceRepository,
      TransactionTemplate transactionTemplate, WaitNotifyEngine waitNotifyEngine) {
    this.approvalInstanceRepository = approvalInstanceRepository;
    this.transactionTemplate = transactionTemplate;
    this.waitNotifyEngine = waitNotifyEngine;
  }

  @Override
  public ApprovalInstance get(@NotNull String approvalInstanceId) {
    Optional<ApprovalInstance> optional = approvalInstanceRepository.findById(approvalInstanceId);
    if (!optional.isPresent()) {
      throw new InvalidRequestException(String.format("Invalid approval instance id: %s", approvalInstanceId));
    }
    return optional.get();
  }

  @Override
  public void delete(@NotNull String approvalInstanceId) {
    approvalInstanceRepository.deleteById(approvalInstanceId);
  }

  @Override
  public HarnessApprovalInstance expire(@NotNull String approvalInstanceId) {
    return (HarnessApprovalInstance) approvalInstanceRepository.updateFirst(
        new Query(Criteria.where(Mapper.ID_KEY).is(approvalInstanceId))
            .addCriteria(Criteria.where(ApprovalInstanceKeys.status).is(ApprovalStatus.WAITING)),
        new Update().set(ApprovalInstanceKeys.status, ApprovalStatus.EXPIRED));
  }

  @Override
  public void markExpiredInstances() {
    approvalInstanceRepository.updateMulti(
        new Query(Criteria.where(ApprovalInstanceKeys.status).is(ApprovalStatus.WAITING))
            .addCriteria(Criteria.where(ApprovalInstanceKeys.deadline).lt(System.currentTimeMillis())),
        new Update().set(ApprovalInstanceKeys.status, ApprovalStatus.EXPIRED));
  }

  @Override
  public HarnessApprovalInstance addHarnessApprovalActivity(@NotNull String approvalInstanceId,
      @NotNull EmbeddedUser user, @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    HarnessApprovalInstance instance =
        doTransaction(status -> addHarnessApprovalActivityInTransaction(approvalInstanceId, user, request));
    if (instance.getStatus() == ApprovalStatus.APPROVED || instance.getStatus() == ApprovalStatus.REJECTED) {
      waitNotifyEngine.doneWith(
          instance.getId(), HarnessApprovalResponseData.builder().approvalInstanceId(instance.getId()).build());
    }
    return instance;
  }

  private HarnessApprovalInstance addHarnessApprovalActivityInTransaction(@NotNull String approvalInstanceId,
      @NotNull EmbeddedUser user, @NotNull @Valid HarnessApprovalActivityRequestDTO request) {
    HarnessApprovalInstance instance = fetchWaitingHarnessApproval(approvalInstanceId);
    if (System.currentTimeMillis() > instance.getDeadline()) {
      throw new InvalidRequestException("Harness approval instance has already expired");
    }

    if (request.getAction() == HarnessApprovalAction.REJECT) {
      instance.setStatus(ApprovalStatus.REJECTED);
    } else {
      int newCount = (instance.getApprovalActivities() == null ? 0 : instance.getApprovalActivities().size()) + 1;
      instance.setStatus(
          (newCount >= instance.getApprovers().getMinimumCount()) ? ApprovalStatus.APPROVED : ApprovalStatus.WAITING);
    }
    instance.addApprovalActivity(user, request);
    return approvalInstanceRepository.save(instance);
  }

  private HarnessApprovalInstance fetchWaitingHarnessApproval(String approvalInstanceId) {
    ApprovalInstance tmpInstance = get(approvalInstanceId);
    if (tmpInstance == null || tmpInstance.getType() != ApprovalType.HARNESS_APPROVAL) {
      throw new InvalidRequestException(String.format("Invalid harness approval instance id: %s", approvalInstanceId));
    }

    HarnessApprovalInstance instance = (HarnessApprovalInstance) tmpInstance;
    if (instance.getStatus() == ApprovalStatus.EXPIRED) {
      throw new InvalidRequestException("Harness approval instance has already expired");
    }
    if (instance.getStatus() != ApprovalStatus.WAITING) {
      throw new InvalidRequestException(
          String.format("Harness approval instance has already completed. Status: %s", instance.getStatus()));
    }
    return instance;
  }

  private <T> T doTransaction(TransactionCallback<T> callback) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(callback));
  }
}
