package io.harness.steps.approval.step;

import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ApprovalInstanceRepository;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.entities.ApprovalInstance.ApprovalInstanceKeys;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivity;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityRequestDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance.HarnessApprovalInstanceKeys;

import com.google.inject.Inject;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.mapping.Mapper;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
public class ApprovalInstanceServiceImpl implements ApprovalInstanceService {
  private final ApprovalInstanceRepository approvalInstanceRepository;

  @Inject
  public ApprovalInstanceServiceImpl(ApprovalInstanceRepository approvalInstanceRepository) {
    this.approvalInstanceRepository = approvalInstanceRepository;
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
  public HarnessApprovalInstance addHarnessApprovalActivity(@NotNull String approvalInstanceId,
      @NotNull ApprovalStatus status, @NotNull EmbeddedUser user, HarnessApprovalActivityRequestDTO request) {
    Update update = new Update().set(ApprovalInstanceKeys.status, status);
    if (request != null) {
      update = update.addToSet(HarnessApprovalInstanceKeys.approvalActivities,
          HarnessApprovalActivity.fromHarnessApprovalActivityRequestDTO(user, request));
    }
    return (HarnessApprovalInstance) approvalInstanceRepository.update(
        new Query(Criteria.where(Mapper.ID_KEY).is(approvalInstanceId))
            .addCriteria(Criteria.where(ApprovalInstanceKeys.type).is(ApprovalType.HARNESS_APPROVAL)),
        update);
  }
}
