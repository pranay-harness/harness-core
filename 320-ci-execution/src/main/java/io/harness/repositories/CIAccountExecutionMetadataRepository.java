package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;

import ci.pipeline.execution.CIAccountExecutionMetadata;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(CI)
public interface CIAccountExecutionMetadataRepository
    extends PagingAndSortingRepository<CIAccountExecutionMetadata, String>, CIAccountExecutionMetadataRepositoryCustom {
  Optional<CIAccountExecutionMetadata> findByAccountId(String accountId);
}
