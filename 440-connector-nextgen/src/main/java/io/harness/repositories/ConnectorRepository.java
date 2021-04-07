package io.harness.repositories;

import io.harness.connector.entities.Connector;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;

import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

@GitSyncableHarnessRepo
@Transactional
//@RepositoryDefinition(domainClass = Connector.class, idClass = String.class)
public interface ConnectorRepository extends Repository<Connector, String>, ConnectorCustomRepository {}
