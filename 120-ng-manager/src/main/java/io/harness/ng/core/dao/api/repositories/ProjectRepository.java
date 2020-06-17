package io.harness.ng.core.dao.api.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.Project;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String> {
  Optional<Project> findByIdAndDeletedNot(String id, boolean deleted);
}
