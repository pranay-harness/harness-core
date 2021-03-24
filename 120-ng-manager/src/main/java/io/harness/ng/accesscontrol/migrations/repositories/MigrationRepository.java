package io.harness.ng.accesscontrol.migrations.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.accesscontrol.migrations.models.Migration;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface MigrationRepository extends PagingAndSortingRepository<Migration, String> {}
