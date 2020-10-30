package io.harness.ng.core.environment.services;

import io.harness.ng.core.environment.beans.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface EnvironmentService {
  Environment create(Environment environment);

  Optional<Environment> get(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier, boolean deleted);

  // TODO(archit): make it transactional
  Environment update(Environment requestEnvironment);

  // TODO(archit): make it transactional
  Environment upsert(Environment requestEnvironment);

  Page<Environment> list(Criteria criteria, Pageable pageable);

  boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier, Long version);
}
