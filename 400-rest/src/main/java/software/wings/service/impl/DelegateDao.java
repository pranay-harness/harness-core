package software.wings.service.impl;

import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;

public class DelegateDao {
  @Inject private HPersistence persistence;

  public Delegate get(String delegateId) {
    Query<Delegate> query = persistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegateId);
    return query.get();
  }
}
