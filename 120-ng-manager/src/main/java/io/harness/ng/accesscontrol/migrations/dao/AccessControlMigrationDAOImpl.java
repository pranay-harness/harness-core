/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.migrations.dao;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.migrations.repositories.AccessControlMigrationRepository;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationDAOImpl implements AccessControlMigrationDAO {
  private final AccessControlMigrationRepository migrationRepository;

  @Override
  public AccessControlMigration save(AccessControlMigration accessControlMigration) {
    return migrationRepository.save(accessControlMigration);
  }

  @Override
  public boolean isMigrated(Scope scope) {
    return migrationRepository
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier())
        .isPresent();
  }
}
