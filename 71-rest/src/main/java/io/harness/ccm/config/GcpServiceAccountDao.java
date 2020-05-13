package io.harness.ccm.config;

import com.google.inject.Inject;

import io.harness.ccm.config.GcpServiceAccount.GcpServiceAccountKeys;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;

public class GcpServiceAccountDao {
  @Inject private HPersistence persistence;

  public String save(GcpServiceAccount gcpServiceAccount) {
    return persistence.save(gcpServiceAccount);
  }

  public GcpServiceAccount getByServiceAccountId(String serviceAccountId) {
    Query<GcpServiceAccount> query = persistence.createQuery(GcpServiceAccount.class)
                                         .field(GcpServiceAccountKeys.serviceAccountId)
                                         .equal(serviceAccountId);
    return query.get();
  }
}
