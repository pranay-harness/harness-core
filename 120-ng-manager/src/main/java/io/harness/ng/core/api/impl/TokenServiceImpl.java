package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.BCryptVersion.$2A;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.api.TokenService;
import io.harness.ng.core.dto.TokenDTO;
import io.harness.ng.core.entities.ApiKey;
import io.harness.ng.core.entities.Token;
import io.harness.ng.core.events.TokenCreateEvent;
import io.harness.ng.core.events.TokenDeleteEvent;
import io.harness.ng.core.events.TokenUpdateEvent;
import io.harness.ng.core.mapper.TokenDTOMapper;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.core.spring.TokenRepository;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@OwnedBy(PL)
public class TokenServiceImpl implements TokenService {
  @Inject private TokenRepository tokenRepository;
  @Inject private ApiKeyService apiKeyService;
  @Inject private OutboxService outboxService;

  private static final String deliminator = ".";

  @Override
  public String createToken(TokenDTO tokenDTO) {
    String randomString = RandomStringUtils.random(20, 0, 0, true, true, null, new SecureRandom());
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder($2A, 10);
    String tokenString = passwordEncoder.encode(randomString);
    String identifier = passwordEncoder.encode(tokenString);
    ApiKey apiKey = apiKeyService.getApiKey(tokenDTO.getAccountIdentifier(), tokenDTO.getOrgIdentifier(),
        tokenDTO.getProjectIdentifier(), tokenDTO.getApiKeyType(), tokenDTO.getParentIdentifier(),
        tokenDTO.getApiKeyIdentifier());
    tokenDTO.setIdentifier(identifier);
    Token token = TokenDTOMapper.getTokenFromDTO(tokenDTO, apiKey.getDefaultTimeToExpireToken());
    token = tokenRepository.save(token);
    outboxService.save(new TokenCreateEvent(TokenDTOMapper.getDTOFromToken(token)));

    tokenString = token.getUuid() + deliminator + tokenString;
    return tokenString;
  }

  @Override
  public boolean revokeToken(String tokenIdentifier) {
    Optional<Token> optionalToken = tokenRepository.findByIdentifier(tokenIdentifier);
    Preconditions.checkState(optionalToken.isPresent(), "No token present with identifier: " + tokenIdentifier);
    long deleted = tokenRepository.deleteByIdentifier(tokenIdentifier);
    if (deleted > 0) {
      outboxService.save(new TokenDeleteEvent(TokenDTOMapper.getDTOFromToken(optionalToken.get())));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String rotateToken(String tokenIdentifier, Instant scheduledExpireTime) {
    Optional<Token> optionalToken = tokenRepository.findByIdentifier(tokenIdentifier);
    Preconditions.checkState(optionalToken.isPresent(), "No token present with identifier: " + tokenIdentifier);
    Token token = optionalToken.get();
    TokenDTO oldToken = TokenDTOMapper.getDTOFromToken(token);
    token.setScheduledExpireTime(scheduledExpireTime);
    token.setValidUntil(new Date(token.getExpiryTimestamp().toEpochMilli()));
    token = tokenRepository.save(token);
    TokenDTO newToken = TokenDTOMapper.getDTOFromToken(token);
    outboxService.save(new TokenUpdateEvent(oldToken, newToken));
    TokenDTO rotatedTokenDTO = TokenDTOMapper.getDTOFromTokenForRotation(token);
    return createToken(rotatedTokenDTO);
  }

  @Override
  public TokenDTO updateToken(TokenDTO tokenDTO) {
    Optional<Token> optionalToken = tokenRepository.findByIdentifier(tokenDTO.getIdentifier());
    Preconditions.checkState(
        optionalToken.isPresent(), "No token present with identifier: " + tokenDTO.getIdentifier());
    Token token = optionalToken.get();
    TokenDTO oldToken = TokenDTOMapper.getDTOFromToken(token);
    token.setName(tokenDTO.getName());
    token.setValidFrom(Instant.ofEpochMilli(tokenDTO.getValidFrom()));
    token.setValidTo(Instant.ofEpochMilli(tokenDTO.getValidTo()));
    token.setValidUntil(new Date(token.getExpiryTimestamp().toEpochMilli()));
    token = tokenRepository.save(token);
    TokenDTO newToken = TokenDTOMapper.getDTOFromToken(token);
    outboxService.save(new TokenUpdateEvent(oldToken, newToken));
    return TokenDTOMapper.getDTOFromToken(token);
  }
}
