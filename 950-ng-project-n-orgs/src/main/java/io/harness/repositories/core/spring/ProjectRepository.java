package io.harness.repositories.core.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.Project;
import io.harness.repositories.core.custom.ProjectRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface ProjectRepository extends PagingAndSortingRepository<Project, String>, ProjectRepositoryCustom {
  Optional<Project> findByAccountIdentifierAndOrgIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String identifier, boolean notDeleted);

  Long countByAccountIdentifier(String accountIdentifier);
}
