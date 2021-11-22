package io.harness.ci.repositories;

import io.harness.ci.pipeline.executions.CIAccountExecutionMetadata;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CIAccountExecutionMetadataRepository
    extends PagingAndSortingRepository<CIAccountExecutionMetadata, String>, CIAccountExecutionMetadataRepositoryCustom {
  Optional<CIAccountExecutionMetadata> findByAccountId(String accountId);
}
