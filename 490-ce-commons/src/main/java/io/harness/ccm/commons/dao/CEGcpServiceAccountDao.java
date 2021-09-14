/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.CEGcpServiceAccount;
import io.harness.ccm.commons.entities.billing.CEGcpServiceAccount.CEGcpServiceAccountKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(CE)
public class CEGcpServiceAccountDao {
  @Inject private HPersistence persistence;

  public String save(CEGcpServiceAccount gcpServiceAccount) {
    return persistence.save(gcpServiceAccount);
  }

  public CEGcpServiceAccount getUnassignedServiceAccountByAccountId(String accountId) {
    Query<CEGcpServiceAccount> query = persistence.createQuery(CEGcpServiceAccount.class)
                                           .field(CEGcpServiceAccountKeys.accountId)
                                           .equal(accountId)
                                           .field(CEGcpServiceAccountKeys.gcpProjectId)
                                           .doesNotExist();
    return query.get();
  }

  public void setProjectId(String serviceAccountEmail, String projectId, String accountId) {
    Query<CEGcpServiceAccount> query = persistence.createQuery(CEGcpServiceAccount.class)
                                           .field(CEGcpServiceAccountKeys.accountId)
                                           .equal(accountId)
                                           .field(CEGcpServiceAccountKeys.email)
                                           .equal(serviceAccountEmail);

    UpdateOperations<CEGcpServiceAccount> updateOperations =
        persistence.createUpdateOperations(CEGcpServiceAccount.class)
            .set(CEGcpServiceAccountKeys.gcpProjectId, projectId);
    persistence.update(query, updateOperations);
  }
}
