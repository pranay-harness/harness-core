package io.harness.ng.core.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.api.repositories.custom.ProjectRepositoryCustom;
import io.harness.ng.core.entities.Project;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String>, ProjectRepositoryCustom {
  Optional<Project> findByAccountIdentifierAndOrgIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String identifier, boolean notDeleted);
}
