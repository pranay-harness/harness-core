package io.harness.repositories.ng.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.models.Secret;
import io.harness.repositories.ng.core.custom.SecretRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface SecretRepository extends PagingAndSortingRepository<Secret, String>, SecretRepositoryCustom {
  Optional<Secret> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
