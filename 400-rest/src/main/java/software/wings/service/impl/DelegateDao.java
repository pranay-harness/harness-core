package software.wings.service.impl;

import io.harness.persistence.HPersistence;

import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;

public class DelegateDao {
  @Inject private HPersistence persistence;

  public Delegate get(String delegateId) {
    Query<Delegate> query = persistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegateId);
    return query.get();
  }
}
