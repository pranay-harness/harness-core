package software.wings.service.impl;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;

public class DelegateDao {
  @Inject private HPersistence persistence;

  public Delegate get(String delegateId) {
    Query<Delegate> query = persistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegateId);
    return query.get();
  }
}
