/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.dao;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.commons.entities.batch.CEDataCleanupRequest;
import io.harness.ccm.commons.entities.batch.CEDataCleanupRequest.CEDataCleanupRequestKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class CEDataCleanupRequestDao {
  private final HPersistence persistence;

  @Inject
  public CEDataCleanupRequestDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public List<CEDataCleanupRequest> getNotProcessedDataCleanupRequests() {
    return persistence.createQuery(CEDataCleanupRequest.class, excludeValidate)
        .field(CEDataCleanupRequestKeys.processedRequest)
        .equal(false)
        .order(CEDataCleanupRequestKeys.createdAt)
        .asList();
  }

  public CEDataCleanupRequest updateRequestStatus(CEDataCleanupRequest ceDataCleanupRequest) {
    Query<CEDataCleanupRequest> query = persistence.createQuery(CEDataCleanupRequest.class, excludeValidate)
                                            .filter(CEDataCleanupRequestKeys.uuid, ceDataCleanupRequest.getUuid());

    UpdateOperations<CEDataCleanupRequest> updateOperations =
        persistence.createUpdateOperations(CEDataCleanupRequest.class);
    updateOperations.set(CEDataCleanupRequestKeys.processedRequest, true);

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public String save(CEDataCleanupRequest ceDataCleanupRequest) {
    if (ceDataCleanupRequest.getAccountId() == null || ceDataCleanupRequest.getBatchJobType() == null
        || ceDataCleanupRequest.getStartAt() == null) {
      throw new InvalidRequestException("Not all required details were entered");
    }
    return persistence.save(ceDataCleanupRequest);
  }

  public boolean delete(String uuid) {
    return persistence.delete(CEDataCleanupRequest.class, uuid);
  }
}
