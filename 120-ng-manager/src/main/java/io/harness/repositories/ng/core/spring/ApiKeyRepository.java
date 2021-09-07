package io.harness.repositories.ng.core.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.entities.ApiKey;
import io.harness.repositories.ng.core.custom.ApiKeyCustomRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface ApiKeyRepository extends PagingAndSortingRepository<ApiKey, String>, ApiKeyCustomRepository {
  Optional<ApiKey>
  findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String identifier);
  long deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String identifier);
  List<ApiKey> findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier);
  List<ApiKey>
  findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifierIn(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, List<String> identifiers);
  long deleteAllByAccountIdentifierAndOrgIdentifierAndParentIdentifierAndApiKeyTypeAndParentIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier);
  long countByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String parentIdentifier);
}
