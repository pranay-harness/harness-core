package io.harness.repositories.ng.core.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.entities.Token;
import io.harness.repositories.ng.core.custom.TokenCustomRepository;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface TokenRepository extends PagingAndSortingRepository<Token, String>, TokenCustomRepository {
  long deleteAllByAccountIdentifierAndOrgIdentifierAndParentIdentifierAndApiKeyTypeAndParentIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier);
  long
  deleteAllByAccountIdentifierAndOrgIdentifierAndParentIdentifierAndApiKeyTypeAndParentIdentifierAndApiKeyIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String apiKeyIdentifier);
  Optional<Token>
  findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndApiKeyIdentifierAndIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String apiKeyIdentifier, String identifier);
  long countByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndApiKeyIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String apiKeyIdentifier);
}
