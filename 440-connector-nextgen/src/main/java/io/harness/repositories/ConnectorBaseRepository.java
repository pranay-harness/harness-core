package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@GitSyncableHarnessRepo
@NoRepositoryBean
@OwnedBy(DX)
public interface ConnectorBaseRepository extends Repository<Connector, ConnectorDTO> {}
