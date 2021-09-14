/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.user.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserMetadata;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface UserMetadataRepositoryCustom {
  Page<UserMetadata> findAll(Criteria criteria, Pageable pageable);

  List<UserMetadata> findAll(Criteria criteria);

  UserMetadata updateFirst(String userId, Update update);

  long insertAllIgnoringDuplicates(List<UserMetadata> userMetadata);
}
