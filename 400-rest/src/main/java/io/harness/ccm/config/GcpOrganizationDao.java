/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.config.GcpOrganization.GcpOrganizationKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(CE)
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
    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
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

  public long count(String accountId) {
    return persistence.createQuery(GcpOrganization.class).field(GcpOrganizationKeys.accountId).equal(accountId).count();
  }

  public boolean delete(String accountId, String uuid) {
    Query<GcpOrganization> query = persistence.createQuery(GcpOrganization.class)
                                       .field(GcpOrganizationKeys.accountId)
                                       .equal(accountId)
                                       .filter(GcpOrganizationKeys.uuid, new ObjectId(uuid));
    return persistence.delete(query);
  }
}
