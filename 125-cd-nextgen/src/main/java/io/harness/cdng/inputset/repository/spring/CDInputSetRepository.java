package io.harness.cdng.inputset.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.repository.custom.CDInputSetRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface CDInputSetRepository
    extends PagingAndSortingRepository<CDInputSetEntity, String>, CDInputSetRepositoryCustom {
  Optional<CDInputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier, boolean notDeleted);
}
