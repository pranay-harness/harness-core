package io.harness.ngpipeline.overlayinputset.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.repository.custom.OverlayInputSetRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface OverlayInputSetRepository
    extends PagingAndSortingRepository<OverlayInputSetEntity, String>, OverlayInputSetRepositoryCustom {
  Optional<OverlayInputSetEntity>
  findByAccountIdAndOrgIdentifierAndProjectIdentifierAndPipelineIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier, boolean notDeleted);
}
