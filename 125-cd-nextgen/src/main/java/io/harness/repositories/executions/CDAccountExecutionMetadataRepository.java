package io.harness.repositories.executions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.executions.CDAccountExecutionMetadata;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PIPELINE)
public interface CDAccountExecutionMetadataRepository
    extends PagingAndSortingRepository<CDAccountExecutionMetadata, String>, CDAccountExecutionMetadataRepositoryCustom {
  Optional<CDAccountExecutionMetadata> findByAccountId(String accountId);
}
