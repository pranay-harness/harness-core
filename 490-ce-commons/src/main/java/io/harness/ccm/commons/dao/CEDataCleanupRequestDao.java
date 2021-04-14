package io.harness.ccm.commons.dao;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.commons.entities.CEDataCleanupRequest;
import io.harness.ccm.commons.entities.CEDataCleanupRequest.CEDataCleanupRequestKeys;
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
    return persistence.createQuery(CEDataCleanupRequest.class)
        .field(CEDataCleanupRequestKeys.processedRequest)
        .doesNotExist()
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
}
