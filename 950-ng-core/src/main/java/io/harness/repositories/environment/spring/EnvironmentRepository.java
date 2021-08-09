package io.harness.repositories.environment.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.repositories.environment.custom.EnvironmentRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface EnvironmentRepository
    extends PagingAndSortingRepository<Environment, String>, EnvironmentRepositoryCustom {
  Optional<Environment> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean notDeleted);

  void deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier);
}
