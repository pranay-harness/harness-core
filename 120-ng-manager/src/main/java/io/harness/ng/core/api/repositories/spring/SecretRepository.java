package io.harness.ng.core.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.api.repositories.custom.SecretRepositoryCustom;
import io.harness.ng.core.models.Secret;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface SecretRepository extends PagingAndSortingRepository<Secret, String>, SecretRepositoryCustom {
  Optional<Secret> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
