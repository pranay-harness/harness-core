package io.harness.connector.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.Connector;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
@NoRepositoryBean
public interface ConnectorBaseRepository<T extends Connector> extends PagingAndSortingRepository<T, String> {
  Optional<T> findByFullyQualifiedIdentifier(String identifier);
  void deleteByFullyQualifiedIdentifier(String identifier);
}
