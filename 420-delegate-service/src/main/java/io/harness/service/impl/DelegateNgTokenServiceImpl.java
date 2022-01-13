/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.impl;

import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateNgToken;
import io.harness.delegate.beans.DelegateNgToken.DelegateNgTokenKeys;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenDetails.DelegateTokenDetailsBuilder;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.delegate.dto.DelegateNgTokenDTO;
import io.harness.delegate.events.DelegateNgTokenCreateEvent;
import io.harness.delegate.events.DelegateNgTokenRevokeEvent;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateNgTokenService;
import io.harness.utils.Misc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
@ValidateOnExecution
@OwnedBy(HarnessTeam.DEL)
public class DelegateNgTokenServiceImpl implements DelegateNgTokenService {
  @Inject private HPersistence persistence;
  @Inject private OutboxService outboxService;

  @Override
  public DelegateTokenDetails createToken(String accountId, DelegateEntityOwner owner, String name) {
    DelegateNgToken delegateToken = DelegateNgToken.builder()
                                        .accountId(accountId)
                                        .owner(owner)
                                        .name(name)
                                        .status(DelegateTokenStatus.ACTIVE)
                                        .value(encodeBase64(Misc.generateSecretKey()))
                                        .build();
    if (!matchNameTokenQuery(accountId, owner, name).asList().isEmpty()) {
      throw new InvalidRequestException("Token with given name already exists for given account, org and project");
    }
    persistence.save(delegateToken);
    publishCreateTokenAuditEvent(delegateToken);
    return getDelegateTokenDetails(delegateToken, true);
  }

  @Override
  public DelegateTokenDetails revokeDelegateToken(String accountId, DelegateEntityOwner owner, String tokenName) {
    Query<DelegateNgToken> filterQuery = matchNameTokenQuery(accountId, owner, tokenName);
    validateTokenToBeRevoked(filterQuery.get());
    UpdateOperations<DelegateNgToken> updateOperations =
        persistence.createUpdateOperations(DelegateNgToken.class)
            .set(DelegateNgTokenKeys.status, DelegateTokenStatus.REVOKED)
            .set(DelegateNgTokenKeys.validUntil,
                Date.from(OffsetDateTime.now().plusDays(DelegateToken.TTL.toDays()).toInstant()));
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(false).returnNew(true);
    DelegateNgToken updatedDelegateToken =
        persistence.findAndModify(filterQuery, updateOperations, findAndModifyOptions);
    publishRevokeTokenAuditEvent(updatedDelegateToken);
    return getDelegateTokenDetails(updatedDelegateToken, false);
  }

  @Override
  public List<DelegateTokenDetails> getDelegateTokens(
      String accountId, DelegateEntityOwner owner, DelegateTokenStatus status) {
    Query<DelegateNgToken> query = nameStartsWithTokenQuery(accountId, owner, null);
    if (null != status) {
      query = query.field(DelegateNgTokenKeys.status).equal(status);
    }
    List<DelegateNgToken> queryResult = query.asList();
    return queryResult.stream().map(token -> getDelegateTokenDetails(token, false)).collect(Collectors.toList());
  }

  @Override
  public DelegateTokenDetails getDelegateToken(String accountId, DelegateEntityOwner owner, String name) {
    Query<DelegateNgToken> query = matchNameTokenQuery(accountId, owner, name);
    List<DelegateNgToken> queryResult = query.asList();
    return queryResult.stream().map(token -> getDelegateTokenDetails(token, false)).findFirst().orElse(null);
  }

  @Override
  public String getDelegateTokenValue(String accountId, DelegateEntityOwner owner, String name) {
    Query<DelegateNgToken> query = matchNameTokenQuery(accountId, owner, name);
    List<DelegateNgToken> queryResult = query.asList();
    DelegateTokenDetails delegateTokenDetails =
        queryResult.stream().map(token -> getDelegateTokenDetails(token, true)).findFirst().orElse(null);
    return delegateTokenDetails != null ? delegateTokenDetails.getValue() : null;
  }

  @Override
  public DelegateTokenDetails upsertDefaultToken(
      String accountIdentifier, DelegateEntityOwner owner, boolean skipIfExists) {
    Query<DelegateNgToken> query = persistence.createQuery(DelegateNgToken.class)
                                       .filter(DelegateNgTokenKeys.accountId, accountIdentifier)
                                       .filter(DelegateNgTokenKeys.name, DEFAULT_TOKEN_NAME);

    if (null != owner) {
      query = query.filter(DelegateNgTokenKeys.owner, owner);
    }

    if (skipIfExists) {
      Query<DelegateNgToken> queryExistsActive = query.filter(DelegateNgTokenKeys.status, DelegateTokenStatus.ACTIVE);
      Optional<DelegateNgToken> token = queryExistsActive.asList().stream().findAny();
      if (token.isPresent()) {
        log.info("Active default Delegate NG Token already exists for account {}, organization {} and project {}",
            accountIdentifier, extractOrganization(owner), extractProject(owner));
        return getDelegateTokenDetails(token.get(), true);
      }
    }

    UpdateOperations<DelegateNgToken> updateOperations =
        persistence.createUpdateOperations(DelegateNgToken.class)
            .setOnInsert(DelegateNgTokenKeys.uuid, UUIDGenerator.generateUuid())
            .setOnInsert(DelegateNgTokenKeys.accountId, accountIdentifier)
            .set(DelegateNgTokenKeys.name, DEFAULT_TOKEN_NAME)
            .set(DelegateNgTokenKeys.status, DelegateTokenStatus.ACTIVE)
            .set(DelegateNgTokenKeys.value, encodeBase64(Misc.generateSecretKey()));

    DelegateNgToken delegateToken = persistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    log.info("Default Delegate NG Token inserted/updated for account {}, organization {} and project {}",
        accountIdentifier, extractOrganization(owner), extractProject(owner));
    return getDelegateTokenDetails(delegateToken, true);
  }

  @Override
  public List<String> getOrgsWithActiveDefaultDelegateTokens(String accountId) {
    List<DelegateNgToken> defaultTokens = getActiveDefaultTokensForAccount(accountId);
    return defaultTokens.stream()
        .filter(token -> DelegateEntityOwnerHelper.isOrganisation(token.getOwner()))
        .map(token -> token.getOwner().getIdentifier())
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getProjectsWithActiveDefaultDelegateTokens(String accountId) {
    List<DelegateNgToken> defaultTokens = getActiveDefaultTokensForAccount(accountId);
    return defaultTokens.stream()
        .filter(token -> DelegateEntityOwnerHelper.isProject(token.getOwner()))
        .map(token -> token.getOwner().getIdentifier())
        .collect(Collectors.toList());
  }

  private List<DelegateNgToken> getActiveDefaultTokensForAccount(String accountId) {
    return persistence.createQuery(DelegateNgToken.class)
        .field(DelegateNgTokenKeys.accountId)
        .equal(accountId)
        .field(DelegateNgTokenKeys.name)
        .equal(DEFAULT_TOKEN_NAME)
        .field(DelegateNgTokenKeys.status)
        .equal(DelegateTokenStatus.ACTIVE)
        .field(DelegateNgTokenKeys.owner)
        .exists()
        .asList();
  }

  private Query<DelegateNgToken> nameStartsWithTokenQuery(
      String accountId, DelegateEntityOwner owner, String tokenName) {
    Query<DelegateNgToken> query =
        persistence.createQuery(DelegateNgToken.class).field(DelegateNgTokenKeys.accountId).equal(accountId);
    query = query.field(DelegateNgTokenKeys.owner).equal(owner);
    if (!StringUtils.isEmpty(tokenName)) {
      query = query.field(DelegateNgTokenKeys.name).startsWith(tokenName);
    }
    return query;
  }

  private Query<DelegateNgToken> matchNameTokenQuery(String accountId, DelegateEntityOwner owner, String tokenName) {
    Query<DelegateNgToken> query =
        persistence.createQuery(DelegateNgToken.class).field(DelegateNgTokenKeys.accountId).equal(accountId);
    query = query.field(DelegateNgTokenKeys.owner).equal(owner);
    if (!StringUtils.isEmpty(tokenName)) {
      query = query.field(DelegateNgTokenKeys.name).equal(tokenName);
    }
    return query;
  }

  private DelegateTokenDetails getDelegateTokenDetails(DelegateNgToken delegateToken, boolean includeTokenValue) {
    DelegateTokenDetailsBuilder delegateTokenDetailsBuilder = DelegateTokenDetails.builder();

    delegateTokenDetailsBuilder.identifier(delegateToken.getIdentifier())
        .accountId(delegateToken.getAccountId())
        .name(delegateToken.getName())
        .createdAt(delegateToken.getCreatedAt())
        .createdBy(delegateToken.getCreatedBy())
        .status(delegateToken.getStatus());

    if (includeTokenValue) {
      delegateTokenDetailsBuilder.value(decodeBase64ToString(delegateToken.getValue()));
    }

    if (delegateToken.getOwner() != null) {
      delegateTokenDetailsBuilder.ownerIdentifier(delegateToken.getOwner().getIdentifier());
    }

    return delegateTokenDetailsBuilder.build();
  }

  private OutboxEvent publishCreateTokenAuditEvent(DelegateNgToken delegateToken) {
    DelegateNgTokenDTO token = convert(delegateToken);
    return outboxService.save(DelegateNgTokenCreateEvent.builder().token(token).build());
  }

  private OutboxEvent publishRevokeTokenAuditEvent(DelegateNgToken delegateToken) {
    DelegateNgTokenDTO token = convert(delegateToken);
    return outboxService.save(DelegateNgTokenRevokeEvent.builder().token(token).build());
  }

  private DelegateNgTokenDTO convert(DelegateNgToken delegateToken) {
    return DelegateNgTokenDTO.builder()
        .accountIdentifier(delegateToken.getAccountId())
        .orgIdentifier(DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(
            delegateToken.getOwner() != null ? delegateToken.getOwner().getIdentifier() : null))
        .projectIdentifier(DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(
            delegateToken.getOwner() != null ? delegateToken.getOwner().getIdentifier() : null))
        .name(delegateToken.getName())
        .identifier(delegateToken.getIdentifier())
        .build();
  }

  private void validateTokenToBeRevoked(DelegateNgToken delegateToken) {
    if (delegateToken == null) {
      throw new InvalidRequestException("Specified token does not exist.");
    }
    if (DelegateTokenStatus.REVOKED.equals(delegateToken.getStatus())) {
      throw new InvalidRequestException("Specified token is already revoked.");
    }
  }

  private String extractOrganization(DelegateEntityOwner owner) {
    return owner != null ? DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(owner.getIdentifier())
                         : StringUtils.EMPTY;
  }

  private String extractProject(DelegateEntityOwner owner) {
    return owner != null ? DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(owner.getIdentifier())
                         : StringUtils.EMPTY;
  }
}
