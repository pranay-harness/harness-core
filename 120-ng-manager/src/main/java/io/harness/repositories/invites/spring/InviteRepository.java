/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.invites.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.repositories.invites.custom.InviteRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface InviteRepository extends PagingAndSortingRepository<Invite, String>, InviteRepositoryCustom {
  Optional<Invite> findFirstByIdAndDeleted(String id, Boolean notDeleted);

  Optional<Invite> findFirstByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndEmailAndDeletedFalse(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email);
}
