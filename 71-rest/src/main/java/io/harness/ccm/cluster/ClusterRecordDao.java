package io.harness.ccm.cluster;

import static io.harness.persistence.HQuery.excludeValidate;

import com.google.inject.Inject;

import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

@Slf4j
public class ClusterRecordDao {
  private final HPersistence persistence;

  public static final String cloudProviderField = ClusterRecordKeys.cluster + "."
      + "cloudProviderId";

  @Inject
  public ClusterRecordDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  private Query<ClusterRecord> getQuery(ClusterRecord clusterRecord) {
    Cluster cluster = clusterRecord.getCluster();
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .filter(ClusterRecordKeys.accountId, clusterRecord.getAccountId());
    cluster.addRequiredQueryFilters(query); // the filter differs depending on the Cluster type
    return query;
  }

  public ClusterRecord upsertCluster(ClusterRecord clusterRecord) {
    Query<ClusterRecord> query = getQuery(clusterRecord);
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class)
            .set(ClusterRecordKeys.accountId, clusterRecord.getAccountId())
            .set(ClusterRecordKeys.cluster, clusterRecord.getCluster());
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public ClusterRecord get(ClusterRecord clusterRecord) {
    Query<ClusterRecord> query = getQuery(clusterRecord);
    return query.get();
  }

  public List<ClusterRecord> list(String accountId, String cloudProviderId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId)
                                     .field(cloudProviderField)
                                     .equal(cloudProviderId);
    return query.asList();
  }

  public boolean delete(ClusterRecord clusterRecord) {
    Query<ClusterRecord> query = getQuery(clusterRecord);
    return persistence.delete(query);
  }

  public boolean delete(String accountId, String cloudProviderId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field(ClusterRecordKeys.accountId)
                                     .equal(accountId)
                                     .field(cloudProviderField)
                                     .equal(cloudProviderId);
    return persistence.delete(query);
  }

  public ClusterRecord insertTask(ClusterRecord clusterRecord, String taskId) {
    Query<ClusterRecord> query = getQuery(clusterRecord);
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).addToSet(ClusterRecordKeys.perpetualTaskIds, taskId);
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(false).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public ClusterRecord removeTask(ClusterRecord clusterRecord, String taskId) {
    Query<ClusterRecord> query = getQuery(clusterRecord);
    UpdateOperations<ClusterRecord> updateOperations =
        persistence.createUpdateOperations(ClusterRecord.class).removeAll(ClusterRecordKeys.perpetualTaskIds, taskId);
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(false).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }
}
