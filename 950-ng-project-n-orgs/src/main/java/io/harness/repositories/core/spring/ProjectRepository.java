package io.harness.repositories.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.Project;
import io.harness.repositories.core.custom.ProjectRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String>, ProjectRepositoryCustom {
  Optional<Project> findByAccountIdentifierAndOrgIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String identifier, boolean notDeleted);
}
