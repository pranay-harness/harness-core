package io.harness.ccm.config;

import com.google.inject.Inject;

import io.harness.ccm.config.GcpOrganization.GcpOrganizationKeys;
import io.harness.persistence.HPersistence;
import org.bson.types.ObjectId;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

public class GcpOrganizationDao {
  @Inject private HPersistence persistence;

  public GcpOrganization upsert(GcpOrganization gcpOrganization) {
    Query<GcpOrganization> query = persistence.createQuery(GcpOrganization.class)
                                       .filter(GcpOrganizationKeys.accountId, gcpOrganization.getAccountId());
    UpdateOperations<GcpOrganization> updateOperations = persistence.createUpdateOperations(GcpOrganization.class);

    if (null != gcpOrganization.getOrganizationId()) {
      updateOperations.set(GcpOrganizationKeys.organizationId, gcpOrganization.getOrganizationId());
    }

    if (null != gcpOrganization.getOrganizationName()) {
      updateOperations.set(GcpOrganizationKeys.organizationName, gcpOrganization.getOrganizationName());
    }

    if (null != gcpOrganization.getServiceAccountEmail()) {
      updateOperations.set(GcpOrganizationKeys.serviceAccountEmail, gcpOrganization.getServiceAccountEmail());
    }
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public String save(GcpOrganization organization) {
    return persistence.save(organization);
  }

  public GcpOrganization get(String uuid) {
    return persistence.createQuery(GcpOrganization.class).filter(GcpOrganizationKeys.uuid, new ObjectId(uuid)).get();
  }

  public List<GcpOrganization> list(String accountId) {
    Query<GcpOrganization> query =
        persistence.createQuery(GcpOrganization.class).field(GcpOrganizationKeys.accountId).equal(accountId);
    return query.asList();
  }

  public boolean delete(String uuid) {
    return persistence.delete(
        persistence.createQuery(GcpOrganization.class).filter(GcpOrganizationKeys.uuid, new ObjectId(uuid)));
  }
}
