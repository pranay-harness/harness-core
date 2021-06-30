package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.ng.core.common.beans.ApiKeyType.SERVICE_ACCOUNT;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.NgManagerTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.AccountOrgProjectValidator;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.api.TokenService;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.entities.ApiKey;
import io.harness.ng.core.entities.Token;
import io.harness.ng.core.mapper.TokenDTOMapper;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.core.spring.TokenRepository;
import io.harness.rule.Owner;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class TokenServiceImplTest extends NgManagerTestBase {
  private TokenService tokenService;
  private TokenRepository tokenRepository;
  private ApiKeyService apiKeyService;
  private OutboxService outboxService;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String identifier;
  private String parentIdentifier;
  private TokenDTO tokenDTO;
  private AccountOrgProjectValidator accountOrgProjectValidator;
  private TransactionTemplate transactionTemplate;
  private Token token;

  @Before
  public void setup() throws IllegalAccessException {
    accountIdentifier = randomAlphabetic(10);
    orgIdentifier = randomAlphabetic(10);
    projectIdentifier = randomAlphabetic(10);
    identifier = randomAlphabetic(10);
    parentIdentifier = randomAlphabetic(10);
    tokenRepository = mock(TokenRepository.class);
    tokenService = new TokenServiceImpl();
    apiKeyService = mock(ApiKeyService.class);
    outboxService = mock(OutboxService.class);
    accountOrgProjectValidator = mock(AccountOrgProjectValidator.class);
    transactionTemplate = mock(TransactionTemplate.class);

    tokenDTO = TokenDTO.builder()
                   .accountIdentifier(accountIdentifier)
                   .orgIdentifier(orgIdentifier)
                   .name(randomAlphabetic(10))
                   .projectIdentifier(projectIdentifier)
                   .identifier(identifier)
                   .parentIdentifier(parentIdentifier)
                   .apiKeyIdentifier(randomAlphabetic(10))
                   .apiKeyType(SERVICE_ACCOUNT)
                   .scheduledExpireTime(Instant.now().toEpochMilli())
                   .build();
    token = Token.builder()
                .scheduledExpireTime(Instant.now())
                .validTo(Instant.now())
                .validFrom(Instant.now())
                .accountIdentifier(accountIdentifier)
                .orgIdentifier(orgIdentifier)
                .name(randomAlphabetic(10))
                .projectIdentifier(projectIdentifier)
                .identifier(identifier)
                .parentIdentifier(parentIdentifier)
                .apiKeyIdentifier(randomAlphabetic(10))
                .apiKeyType(SERVICE_ACCOUNT)
                .build();
    token.setUuid(generateUuid());
    when(accountOrgProjectValidator.isPresent(any(), any(), any())).thenReturn(true);
    when(transactionTemplate.execute(any())).thenReturn(token);
    FieldUtils.writeField(tokenService, "tokenRepository", tokenRepository, true);
    FieldUtils.writeField(tokenService, "apiKeyService", apiKeyService, true);
    FieldUtils.writeField(tokenService, "outboxService", outboxService, true);
    FieldUtils.writeField(tokenService, "accountOrgProjectValidator", accountOrgProjectValidator, true);
    FieldUtils.writeField(tokenService, "transactionTemplate", transactionTemplate, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreateToken() {
    ApiKey apiKey = ApiKey.builder().defaultTimeToExpireToken(Duration.ofDays(2).toMillis()).build();
    apiKey.setUuid(randomAlphabetic(10));
    doReturn(apiKey).when(apiKeyService).getApiKey(any(), any(), any(), any(), any(), any());
    Token newToken = TokenDTOMapper.getTokenFromDTO(tokenDTO, Duration.ofDays(2).toMillis());
    newToken.setUuid(randomAlphabetic(10));
    doReturn(newToken).when(tokenRepository).save(any());
    String tokenString = tokenService.createToken(tokenDTO);
    assertThat(tokenString).startsWith(token.getUuid());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testRotateToken() {
    ApiKey apiKey = ApiKey.builder().defaultTimeToExpireToken(Duration.ofDays(2).toMillis()).build();
    apiKey.setUuid(randomAlphabetic(10));
    doReturn(apiKey).when(apiKeyService).getApiKey(any(), any(), any(), any(), any(), any());
    doReturn(Optional.of(TokenDTOMapper.getTokenFromDTO(tokenDTO, Duration.ofDays(2).toMillis())))
        .when(tokenRepository)
        .findByIdentifier(any());
    Token newToken = TokenDTOMapper.getTokenFromDTO(tokenDTO, Duration.ofDays(2).toMillis());
    newToken.setUuid(randomAlphabetic(10));
    doReturn(newToken).when(tokenRepository).save(any());
    String tokenString = tokenService.rotateToken(identifier, Instant.now().plusMillis(1000));
    assertThat(tokenString).startsWith(token.getUuid());
  }
}
