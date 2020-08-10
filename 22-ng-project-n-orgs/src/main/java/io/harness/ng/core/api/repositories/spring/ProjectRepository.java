package io.harness.ng.core.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.api.repositories.custom.ProjectRepositoryCustom;
import io.harness.ng.core.entities.Project;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String>, ProjectRepositoryCustom {
  Optional<Project> findByOrgIdentifierAndIdentifierAndDeletedNot(
      String orgIdentifier, String projectIdentifier, boolean notDeleted);
}
