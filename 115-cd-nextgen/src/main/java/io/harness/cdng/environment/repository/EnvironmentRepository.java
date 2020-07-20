package io.harness.cdng.environment.repository;

import io.harness.annotation.HarnessRepo;
import io.harness.cdng.environment.beans.Environment;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface EnvironmentRepository extends PagingAndSortingRepository<Environment, String> {}
