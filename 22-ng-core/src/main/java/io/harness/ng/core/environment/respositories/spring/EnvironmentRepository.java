package io.harness.ng.core.environment.respositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.respositories.custom.EnvironmentRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface EnvironmentRepository
    extends PagingAndSortingRepository<Environment, String>, EnvironmentRepositoryCustom {
  Optional<Environment> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean notDeleted);

  void deleteByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier);
}
