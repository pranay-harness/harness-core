package io.harness.connector.repositories.base;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.Connector;

import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface ConnectorRepository extends ConnectorBaseRepository<Connector>, ConnectorCustomRepository {}
