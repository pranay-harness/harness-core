package io.harness.repositories.filters;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.ConnectorFilter;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface ConnectorFilterRepository
    extends ConnectorFilterCustomRepository, PagingAndSortingRepository<ConnectorFilter, String> {
  long deleteByFullyQualifiedIdentifier(String fullyQualifiedIdentifier);
  Optional<ConnectorFilter> findByFullyQualifiedIdentifier(String fullyQualifiedIdentifier);
}