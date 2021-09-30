package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.config.GcpServiceAccount.GcpServiceAccountKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;

@OwnedBy(CE)
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

  public GcpServiceAccount getByAccountId(String accountId) {
    Query<GcpServiceAccount> query =
        persistence.createQuery(GcpServiceAccount.class).field(GcpServiceAccountKeys.accountId).equal(accountId);
    return query.get();
  }
}
