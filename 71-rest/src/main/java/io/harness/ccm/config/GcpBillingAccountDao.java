package io.harness.ccm.config;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.ccm.config.GcpBillingAccount.GcpBillingAccountKeys;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

public class GcpBillingAccountDao {
  @Inject private HPersistence persistence;

  public String save(GcpBillingAccount billingAccount) {
    return persistence.save(billingAccount);
  }

  public GcpBillingAccount get(String uuid) {
    return persistence.createQuery(GcpBillingAccount.class).field(GcpBillingAccountKeys.uuid).equal(uuid).get();
  }

  public List<GcpBillingAccount> list(String accountId, String organizationSettingId) {
    Query<GcpBillingAccount> query =
        persistence.createQuery(GcpBillingAccount.class).field(GcpBillingAccountKeys.accountId).equal(accountId);
    if (isNotEmpty(organizationSettingId)) {
      query.field(GcpBillingAccountKeys.organizationSettingId).equal(organizationSettingId);
    }
    return query.asList();
  }

  public boolean delete(String billingAccountId) {
    return persistence.delete(GcpBillingAccount.class, billingAccountId);
  }

  public void update(String uuid, GcpBillingAccount billingAccount) {
    Query query = persistence.createQuery(GcpBillingAccount.class).field(GcpBillingAccountKeys.uuid).equal(uuid);
    UpdateOperations<GcpBillingAccount> updateOperations =
        persistence.createUpdateOperations(GcpBillingAccount.class)
            .set(GcpBillingAccountKeys.exportEnabled, billingAccount.isExportEnabled());

    if (null != billingAccount.getGcpBillingAccountId()) {
      updateOperations.set(GcpBillingAccountKeys.gcpBillingAccountId, billingAccount.getGcpBillingAccountId());
    }

    if (null != billingAccount.getGcpBillingAccountName()) {
      updateOperations.set(GcpBillingAccountKeys.gcpBillingAccountName, billingAccount.getGcpBillingAccountName());
    }

    if (null != billingAccount.getBqProjectId()) {
      updateOperations.set(GcpBillingAccountKeys.bqProjectId, billingAccount.getBqProjectId());
    }

    if (null != billingAccount.getBqDatasetId()) {
      updateOperations.set(GcpBillingAccountKeys.bqDatasetId, billingAccount.getBqDatasetId());
    }

    persistence.update(query, updateOperations);
  }
}
