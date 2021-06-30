package io.harness.ccm.commons.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.ClusterRecord;
import io.harness.ccm.commons.entities.ClusterRecord.ClusterRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(CE)
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

  public ClusterRecord get(String accountId, String k8sBaseConnectorRefIdentifier) {
    Query<ClusterRecord> query =
        persistence.createQuery(ClusterRecord.class)
            .filter(ClusterRecordKeys.accountId, accountId)
            .filter(ClusterRecordKeys.k8sBaseConnectorRefIdentifier, k8sBaseConnectorRefIdentifier);
    return query.get();
  }

  public boolean delete(String accountId, String k8sBaseConnectorRefIdentifier) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId)
                                     .field(ClusterRecordKeys.k8sBaseConnectorRefIdentifier)
                                     .equal(k8sBaseConnectorRefIdentifier);
    return persistence.delete(query);
  }

  public ClusterRecord getByCEK8sIdentifier(String accountId, String ceK8sConnectorRefIdentifier) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.accountId, accountId)
                                     .filter(ClusterRecordKeys.ceK8sConnectorIdentifier, ceK8sConnectorRefIdentifier);
    return query.get();
  }

  public ClusterRecord upsert(ClusterRecord clusterRecord) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class)
            .set(ClusterRecordKeys.accountId, clusterRecord.getAccountId())
            .set(ClusterRecordKeys.ceK8sConnectorIdentifier, clusterRecord.getCeK8sConnectorIdentifier())
            .set(ClusterRecordKeys.k8sBaseConnectorRefIdentifier, clusterRecord.getK8sBaseConnectorRefIdentifier())
            .set(ClusterRecordKeys.orgIdentifier, clusterRecord.getOrgIdentifier())
            .set(ClusterRecordKeys.projectIdentifier, clusterRecord.getProjectIdentifier())
            .set(ClusterRecordKeys.perpetualTaskId, clusterRecord.getPerpetualTaskId())
            .set(ClusterRecordKeys.clusterName, clusterRecord.getClusterName());
    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  public ClusterRecord insertTask(ClusterRecord clusterRecord, String taskId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));

    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).addToSet(ClusterRecordKeys.perpetualTaskId, taskId);
    return persistence.findAndModify(query, updateOperations, returnNewOptions);
  }

  public ClusterRecord removeTask(ClusterRecord clusterRecord, String taskId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class)
                                     .filter(ClusterRecordKeys.uuid, new ObjectId(clusterRecord.getUuid()));

    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).removeAll(ClusterRecordKeys.perpetualTaskId, taskId);
    return persistence.findAndModify(query, updateOperations, returnNewOptions);
  }
}
