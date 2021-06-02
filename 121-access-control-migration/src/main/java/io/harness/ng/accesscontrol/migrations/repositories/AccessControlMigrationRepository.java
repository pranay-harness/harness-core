package io.harness.ng.accesscontrol.migrations.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PL)
public interface AccessControlMigrationRepository extends PagingAndSortingRepository<AccessControlMigration, String> {}
