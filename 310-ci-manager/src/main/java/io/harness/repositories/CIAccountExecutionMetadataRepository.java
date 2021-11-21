package io.harness.repositories;

import io.harness.pipeline.CIAccountExecutionMetadata;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CIAccountExecutionMetadataRepository
    extends PagingAndSortingRepository<CIAccountExecutionMetadata, String>, CIAccountExecutionMetadataRepositoryCustom {
  Optional<CIAccountExecutionMetadata> findByAccountId(String accountId);
}
