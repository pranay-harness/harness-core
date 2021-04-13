package io.harness.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenDetails.DelegateTokenDetailsBuilder;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTokenService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenServiceImpl implements DelegateTokenService {
  @Inject private HPersistence persistence;

  @Override
  public DelegateTokenDetails createDelegateToken(String accountId, String name) {
    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(accountId)
                                      .createdAt(System.currentTimeMillis())
                                      .name(name)
                                      .status(DelegateTokenStatus.ACTIVE)
                                      .value(UUIDGenerator.generateUuid())
                                      .build();

    persistence.save(delegateToken);

    return getDelegateTokenDetails(delegateToken, true);
  }

  @Override
  public void revokeDelegateToken(String accountId, String tokenName) {
    Query<DelegateToken> filterQuery = persistence.createQuery(DelegateToken.class)
                                           .field(DelegateTokenKeys.accountId)
                                           .equal(accountId)
                                           .field(DelegateTokenKeys.name)
                                           .equal(tokenName);

    UpdateOperations<DelegateToken> updateOperations = persistence.createUpdateOperations(DelegateToken.class)
                                                           .set(DelegateTokenKeys.status, DelegateTokenStatus.REVOKED);

    persistence.findAndModify(filterQuery, updateOperations, new FindAndModifyOptions());
  }

  @Override
  public void deleteDelegateToken(String accountId, String tokenName) {
    Query<DelegateToken> deleteQuery = persistence.createQuery(DelegateToken.class)
                                           .field(DelegateTokenKeys.accountId)
                                           .equal(accountId)
                                           .field(DelegateTokenKeys.name)
                                           .equal(tokenName);

    persistence.delete(deleteQuery);
  }

  @Override
  public String getTokenValue(String accountId, String tokenName) {
    DelegateToken delegateToken = persistence.createQuery(DelegateToken.class)
                                      .field(DelegateTokenKeys.accountId)
                                      .equal(accountId)
                                      .field(DelegateTokenKeys.name)
                                      .equal(tokenName)
                                      .get();

    return delegateToken != null ? delegateToken.getValue() : null;
  }

  @Override
  public List<DelegateTokenDetails> getDelegateTokens(String accountId, String status, String tokenName) {
    List<DelegateToken> queryResults;

    Query<DelegateToken> query =
        persistence.createQuery(DelegateToken.class).field(DelegateTokenKeys.accountId).equal(accountId);

    if (!StringUtils.isEmpty(status)) {
      query = query.field(DelegateTokenKeys.status).equal(status);
    }

    if (!StringUtils.isEmpty(tokenName)) {
      query = query.field(DelegateTokenKeys.name).equal(tokenName);
    }

    queryResults = query.asList();

    List<DelegateTokenDetails> delegateTokenDetailsList = new ArrayList<>();

    // Removing token values
    queryResults.forEach(token -> delegateTokenDetailsList.add(getDelegateTokenDetails(token, false)));

    return delegateTokenDetailsList;
  }

  private DelegateTokenDetails getDelegateTokenDetails(DelegateToken delegateToken, boolean includeTokenValue) {
    DelegateTokenDetailsBuilder delegateTokenDetailsBuilder = DelegateTokenDetails.builder();

    delegateTokenDetailsBuilder.uuid(delegateToken.getUuid())
        .accountId(delegateToken.getAccountId())
        .name(delegateToken.getName())
        .createdAt(delegateToken.getCreatedAt())
        .createdBy(delegateToken.getCreatedBy())
        .status(delegateToken.getStatus());

    if (includeTokenValue) {
      delegateTokenDetailsBuilder.value(delegateToken.getValue());
    }

    return delegateTokenDetailsBuilder.build();
  }
}
