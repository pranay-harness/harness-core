package software.wings.service.impl;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;

import java.util.List;

@Slf4j
public class DelegateConnectionDao {
  @Inject private HPersistence persistence;

  public String save(DelegateConnection delegateConnection) {
    return persistence.save(delegateConnection);
  }

  public List<DelegateConnection> list(Delegate delegate) {
    return persistence.createQuery(DelegateConnection.class)
        .filter(DelegateConnectionKeys.accountId, delegate.getAccountId())
        .filter(DelegateConnectionKeys.delegateId, delegate.getUuid())
        .asList();
  }
}
