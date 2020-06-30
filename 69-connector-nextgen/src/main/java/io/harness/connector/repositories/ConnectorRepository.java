package io.harness.connector.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.Connector;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface ConnectorRepository extends PagingAndSortingRepository<Connector, String> {
  Optional<Connector> findByFullyQualifiedIdentifier(String identifier);
  void deleteByFullyQualifiedIdentifier(String identifier);
}
