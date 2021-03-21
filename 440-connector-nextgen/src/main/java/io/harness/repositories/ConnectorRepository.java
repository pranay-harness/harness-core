package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.Connector;

import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
@OwnedBy(DX)
public interface ConnectorRepository extends ConnectorBaseRepository<Connector>, ConnectorCustomRepository {}
