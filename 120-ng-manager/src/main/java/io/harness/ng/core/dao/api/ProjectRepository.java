package io.harness.ng.core.dao.api;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.Project;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String> {}
