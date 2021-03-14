package io.harness.ccm.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.config.GcpServiceAccount.GcpServiceAccountKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
@TargetModule(Module._490_CE_COMMONS)
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
