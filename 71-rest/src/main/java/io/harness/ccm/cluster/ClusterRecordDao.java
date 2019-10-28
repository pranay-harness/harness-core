package io.harness.ccm.cluster;

import static io.harness.persistence.HQuery.excludeValidate;

import com.google.inject.Inject;

import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class ClusterRecordDao {
  final HPersistence persistence;

  @Inject
  public ClusterRecordDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public ClusterRecord upsert(ClusterRecord clusterRecord) {
    Cluster cluster = clusterRecord.getCluster();
    Query<ClusterRecord> query =
        persistence.createQuery(ClusterRecord.class, excludeValidate).filter("accountId", clusterRecord.getAccountId());
    // the filter differs depending on the Cluster type
    cluster.addRequiredQueryFilters(query);

    UpdateOperations<ClusterRecord> updateOperations = persistence.createUpdateOperations(ClusterRecord.class)
                                                           .set("accountId", clusterRecord.getAccountId())
                                                           .set("cluster", cluster);
    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);

    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public boolean delete(String accountId, String cloudProviderId) {
    Query<ClusterRecord> query = persistence.createQuery(ClusterRecord.class, excludeValidate)
                                     .field("accountId")
                                     .equal(accountId)
                                     .field("cluster.cloudProviderId")
                                     .equal(cloudProviderId);
    return persistence.delete(query);
  }
}
