/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.ci.beans.entities.CIBuild;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface CIBuildInfoRepositoryCustom {
  Page<CIBuild> findAll(Criteria criteria, Pageable pageable);
  Optional<CIBuild> getBuildById(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Long buildIdentifier);
  Page<CIBuild> getBuilds(Criteria criteria, Pageable pageable);
}
