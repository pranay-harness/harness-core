/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.user.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserProjectMap;
import io.harness.repositories.user.custom.UserProjectMapRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface UserProjectMapRepository
    extends PagingAndSortingRepository<UserProjectMap, String>, UserProjectMapRepositoryCustom {
  Optional<UserProjectMap> findByUserIdAndAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<UserProjectMap> findFirstByUserIdAndAccountIdentifier(String userId, String accountIdentifier);

  Optional<UserProjectMap> findByMigratedExists(boolean exists);
}
