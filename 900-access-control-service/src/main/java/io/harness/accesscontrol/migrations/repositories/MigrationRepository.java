package io.harness.accesscontrol.migrations.repositories;

import io.harness.accesscontrol.migrations.models.Migration;
import io.harness.annotation.HarnessRepo;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface MigrationRepository extends PagingAndSortingRepository<Migration, String> {}
