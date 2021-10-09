package io.harness.ccm.commons.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CEGcpServiceAccount;
import io.harness.ccm.commons.entities.billing.CEGcpServiceAccount.CEGcpServiceAccountKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;

@OwnedBy(CE)
public class CEGcpServiceAccountDao {
  @Inject private HPersistence persistence;

  public String save(CEGcpServiceAccount gcpServiceAccount) {
    return persistence.save(gcpServiceAccount);
  }

  public CEGcpServiceAccount getUnassignedServiceAccountByAccountId(String accountId) {
    Query<CEGcpServiceAccount> query =
        persistence.createQuery(CEGcpServiceAccount.class).field(CEGcpServiceAccountKeys.accountId).equal(accountId);
    return query.get();
  }

  public CEGcpServiceAccount getByServiceAccountId(String serviceAccountEmail) {
    Query<CEGcpServiceAccount> query = persistence.createQuery(CEGcpServiceAccount.class)
                                           .field(CEGcpServiceAccountKeys.email)
                                           .equal(serviceAccountEmail);
    return query.get();
  }
}
