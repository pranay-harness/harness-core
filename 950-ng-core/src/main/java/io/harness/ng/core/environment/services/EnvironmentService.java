/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.environment.services;

import io.harness.ng.core.environment.beans.Environment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

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

  List<Environment> listAccess(Criteria criteria);
}
