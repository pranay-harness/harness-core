package io.harness.repositories.ng.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.repositories.ng.core.custom.NGTriggerRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface NGTriggerRepository
    extends PagingAndSortingRepository<NGTriggerEntity, String>, NGTriggerRepositoryCustom {
  Optional<NGTriggerEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndTargetIdentifierAndIdentifierAndDeletedNot(String accountId,
      String orgIdentifier, String projectIdentifier, String targetIdentifier, String identifier, boolean notDeleted);
}
