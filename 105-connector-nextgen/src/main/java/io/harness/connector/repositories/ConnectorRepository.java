package io.harness.connector.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.Connector;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ConnectorRepository extends PagingAndSortingRepository<Connector, String> {}
