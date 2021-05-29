package io.harness.ccm.commons.dao;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.entities.ClusterRecord.ClusterRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class ClusterRecordDao {
  private final HPersistence persistence;
  @Inject
  public ClusterRecordDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public ClusterRecord get(String clusterId) {
    Query<ClusterRecord> query =
        persistence.createQuery(ClusterRecord.class).filter(ClusterRecordKeys.uuid, new ObjectId(clusterId));
    return query.get();
  }

  public ClusterRecord get(ClusterRecord clusterRecord) {
    Preconditions.checkNotNull(clusterRecord);
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .filter(ClusterRecordKeys.accountId, clusterRecord.getAccountId());
    return query.get();
  }

  public List<ClusterRecord> list(String accountId, String connectorIdentifier, Integer count, Integer startIndex) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId);
    if (!isEmpty(connectorIdentifier)) {
      query.field(ClusterRecordKeys.connectorIdentifier).equal(connectorIdentifier);
    }
    return query.asList(new FindOptions().skip(startIndex).limit(count));
  }

  public List<ClusterRecord> listCeEnabledClusters(String accountId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId)
                                     .field(ClusterRecordKeys.perpetualTaskIds)
                                     .exists();
    return query.asList(new FindOptions());
  }

  public ClusterRecord upsertCluster(ClusterRecord clusterRecord) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class)
            .set(ClusterRecordKeys.accountId, clusterRecord.getAccountId())
            .set(ClusterRecordKeys.connectorIdentifier, clusterRecord.getConnectorIdentifier())
            .set(ClusterRecordKeys.orgIdentifier, clusterRecord.getOrgIdentifier())
            .set(ClusterRecordKeys.projectIdentifier, clusterRecord.getProjectIdentifier())
            .set(ClusterRecordKeys.clusterName, clusterRecord.getClusterName());
    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  public boolean delete(ClusterRecord clusterRecord) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));
    return persistence.delete(query);
  }

  public boolean delete(String accountId, String connectorIdentifier) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId)
                                     .field(ClusterRecordKeys.connectorIdentifier)
                                     .equal(connectorIdentifier);
    return persistence.delete(query);
  }

  public ClusterRecord setStatus(String accountId, String connectorIdentifier, boolean isDeactivated) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId)
                                     .field(ClusterRecordKeys.connectorIdentifier)
                                     .equal(connectorIdentifier);
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).set(ClusterRecordKeys.isDeactivated, isDeactivated);
    return persistence.findAndModify(query, updateOperations, returnNewOptions);
  }

  public ClusterRecord insertTask(ClusterRecord clusterRecord, String taskId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).addToSet(ClusterRecordKeys.perpetualTaskIds, taskId);
    return persistence.findAndModify(query, updateOperations, returnNewOptions);
  }

  public ClusterRecord removeTask(ClusterRecord clusterRecord, String taskId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).removeAll(ClusterRecordKeys.perpetualTaskIds, taskId);
    return persistence.findAndModify(query, updateOperations, returnNewOptions);
  }
}
