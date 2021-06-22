package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.ng.core.common.beans.ApiKeyType.SERVICE_ACCOUNT;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.NgManagerTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.entities.ApiKey;
import io.harness.repositories.ng.core.spring.ApiKeyRepository;
import io.harness.rule.Owner;

import io.fabric8.utils.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class ApiKeyServiceImplTest extends NgManagerTestBase {
  private ApiKeyService apiKeyService;
  private ApiKeyRepository apiKeyRepository;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String identifier;
  private String parentIdentifier;
  private ApiKeyDTO apiKeyDTO;

  @Before
  public void setup() throws IllegalAccessException {
    accountIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    identifier = generateUuid();
    parentIdentifier = generateUuid();
    apiKeyRepository = mock(ApiKeyRepository.class);
    apiKeyService = new ApiKeyServiceImpl();

    apiKeyDTO = ApiKeyDTO.builder()
                    .accountIdentifier(accountIdentifier)
                    .orgIdentifier(orgIdentifier)
                    .projectIdentifier(projectIdentifier)
                    .identifier(identifier)
                    .parentIdentifier(parentIdentifier)
                    .apiKeyType(SERVICE_ACCOUNT)
                    .build();
    FieldUtils.writeField(apiKeyService, "apiKeyRepository", apiKeyRepository, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateApiKey_duplicateIdentifier() {
    doReturn(Optional.of(ApiKey.builder().build()))
        .when(apiKeyRepository)
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, SERVICE_ACCOUNT, parentIdentifier, identifier);

    assertThatThrownBy(() -> apiKeyService.createApiKey(apiKeyDTO))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate api key present in scope for identifier: " + identifier);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testUpdateApiKey_noAccountExists() {
    doReturn(Optional.empty())
        .when(apiKeyRepository)
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, SERVICE_ACCOUNT, parentIdentifier, identifier);

    assertThatThrownBy(() -> apiKeyService.updateApiKey(apiKeyDTO))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Api key not present in scope for identifier: " + identifier);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void listServiceAccountDTO() {
    doReturn(Lists.newArrayList(ApiKey.builder()
                                    .identifier(identifier)
                                    .accountIdentifier(accountIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .build()))
        .when(apiKeyRepository)
        .findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, SERVICE_ACCOUNT, parentIdentifier);
    List<ApiKeyDTO> apiKeys = apiKeyService.listApiKeys(
        accountIdentifier, orgIdentifier, projectIdentifier, SERVICE_ACCOUNT, parentIdentifier, new ArrayList<>());
    assertThat(apiKeys.size()).isEqualTo(1);
  }
}
