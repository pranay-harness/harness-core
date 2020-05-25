package io.harness.ccm.config;

import static com.google.common.base.Preconditions.checkArgument;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.ccm.config.GcpBillingAccount.GcpBillingAccountKeys;
import io.harness.persistence.HPersistence;
import org.bson.types.ObjectId;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

public class GcpBillingAccountDao {
  @Inject private HPersistence persistence;

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

  public boolean delete(String accountId, String organizationSettingId) {
    return delete(accountId, organizationSettingId, null);
  }

  public boolean delete(String accountId, String organizationSettingId, String billingAccountId) {
    checkArgument(isNotEmpty(organizationSettingId));
    Query<GcpBillingAccount> query = persistence.createQuery(GcpBillingAccount.class)
                                         .field(GcpBillingAccountKeys.accountId)
                                         .equal(accountId)
                                         .field(GcpBillingAccountKeys.organizationSettingId)
                                         .equal(organizationSettingId);
    if (isNotEmpty(billingAccountId)) {
      query.field(GcpBillingAccountKeys.uuid).equal(new ObjectId(billingAccountId));
    }
    return persistence.delete(query);
  }

  public GcpBillingAccount upsert(GcpBillingAccount billingAccount) {
    Query<GcpBillingAccount> query =
        persistence.createQuery(GcpBillingAccount.class)
            .filter(GcpBillingAccountKeys.accountId, billingAccount.getAccountId())
            .filter(GcpBillingAccountKeys.organizationSettingId, billingAccount.getOrganizationSettingId());

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

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public void update(String uuid, GcpBillingAccount billingAccount) {
    Query<GcpBillingAccount> query =
        persistence.createQuery(GcpBillingAccount.class).field(GcpBillingAccountKeys.uuid).equal(uuid);
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

  public String save(GcpBillingAccount billingAccount) {
    return persistence.save(billingAccount);
  }
}
