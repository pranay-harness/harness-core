package io.harness.service.impl;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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

  private static final String DEFAULT_TOKEN_NAME = "default";

  @Override
  public DelegateTokenDetails createToken(String accountId, DelegateEntityOwner owner, String name, String identifier) {
    DelegateNgToken delegateToken = DelegateNgToken.builder()
                                        .identifier(identifier)
                                        .accountId(accountId)
                                        .owner(owner)
                                        .name(name)
                                        .status(DelegateTokenStatus.ACTIVE)
                                        .value(encodeBase64(Misc.generateSecretKey()))
                                        .build();
    if (!createTokenQuery(accountId, owner, name).asList().isEmpty()) {
      throw new InvalidRequestException("Token with given name already exists for given account, org and project");
    }
    persistence.save(delegateToken);
    publishCreateTokenAuditEvent(delegateToken);
    return getDelegateTokenDetails(delegateToken, true);
  }

  @Override
  public DelegateTokenDetails revokeDelegateToken(String accountId, DelegateEntityOwner owner, String tokenName) {
    Query<DelegateNgToken> filterQuery = createTokenQuery(accountId, owner, tokenName);
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
    Query<DelegateNgToken> query = createTokenQuery(accountId, owner, null);
    if (null != status) {
      query = query.field(DelegateNgTokenKeys.status).equal(status);
    }
    List<DelegateNgToken> queryResult = query.asList();
    return queryResult.stream().map(token -> getDelegateTokenDetails(token, false)).collect(Collectors.toList());
  }

  private Query<DelegateNgToken> createTokenQuery(String accountId, DelegateEntityOwner owner, String tokenName) {
    Query<DelegateNgToken> query =
        persistence.createQuery(DelegateNgToken.class).field(DelegateNgTokenKeys.accountId).equal(accountId);
    query = query.field(DelegateNgTokenKeys.owner).equal(owner);
    if (!StringUtils.isEmpty(tokenName)) {
      query = query.field(DelegateNgTokenKeys.name).startsWith(tokenName);
    }
    return query;
  }

  private DelegateTokenDetails getDelegateTokenDetails(DelegateNgToken delegateToken, boolean includeTokenValue) {
    DelegateTokenDetailsBuilder delegateTokenDetailsBuilder = DelegateTokenDetails.builder();

    delegateTokenDetailsBuilder.identifier(delegateToken.getIdentifier())
        .accountId(delegateToken.getAccountId())
        .owner(delegateToken.getOwner())
        .name(delegateToken.getName())
        .createdAt(delegateToken.getCreatedAt())
        .createdBy(delegateToken.getCreatedBy())
        .status(delegateToken.getStatus());

    if (includeTokenValue) {
      delegateTokenDetailsBuilder.value(delegateToken.getValue());
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
        .orgIdentifier(
            DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(delegateToken.getOwner().getIdentifier()))
        .projectIdentifier(
            DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(delegateToken.getOwner().getIdentifier()))
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
}
