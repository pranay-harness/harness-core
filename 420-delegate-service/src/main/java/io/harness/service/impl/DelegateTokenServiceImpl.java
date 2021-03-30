package io.harness.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTokenService;

import com.google.inject.Inject;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenServiceImpl implements DelegateTokenService {
  @Inject private HPersistence persistence;

  @Override
  public DelegateToken createDelegateToken(String accountId, String name) {
    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(accountId)
                                      .createdAt(System.currentTimeMillis())
                                      .name(name)
                                      .status(DelegateTokenStatus.ACTIVE)
                                      .value(UUIDGenerator.generateUuid())
                                      .build();

    persistence.save(delegateToken);
    delegateToken.setValue("");

    return delegateToken;
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
}
