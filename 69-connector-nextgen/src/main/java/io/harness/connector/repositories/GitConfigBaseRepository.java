package io.harness.connector.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface GitConfigBaseRepository extends ConnectorBaseRepository<GitConfig> {}
