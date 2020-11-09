package io.harness.ngtriggers.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.repository.custom.NGTriggerRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface NGTriggerRepository
    extends PagingAndSortingRepository<NGTriggerEntity, String>, NGTriggerRepositoryCustom {
  Optional<NGTriggerEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndTargetIdentifierAndIdentifierAndDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String targetIdentifier, String identifier, boolean notDeleted);
}
