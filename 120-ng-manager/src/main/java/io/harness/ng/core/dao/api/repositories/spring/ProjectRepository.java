package io.harness.ng.core.dao.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.dao.api.repositories.custom.ProjectRepositoryCustom;
import io.harness.ng.core.entities.Project;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String>, ProjectRepositoryCustom {
  Optional<Project> findByIdAndDeletedNot(String id, boolean deleted);
}
