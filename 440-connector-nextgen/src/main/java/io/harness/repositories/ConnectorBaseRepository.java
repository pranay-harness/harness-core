package io.harness.repositories;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@GitSyncableHarnessRepo
@NoRepositoryBean
public interface ConnectorBaseRepository extends Repository<Connector, ConnectorDTO> {}
