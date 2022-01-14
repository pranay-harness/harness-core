package io.harness.repositories.executions;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.plan.execution.AccountExecutionMetadata;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PIPELINE)
public interface AccountExecutionMetadataRepository
    extends PagingAndSortingRepository<AccountExecutionMetadata, String>, AccountExecutionMetadataRepositoryCustom {
  Optional<AccountExecutionMetadata> findByAccountId(String accountId);
}
