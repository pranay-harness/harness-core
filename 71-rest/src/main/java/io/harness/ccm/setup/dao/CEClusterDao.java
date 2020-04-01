package io.harness.ccm.setup.dao;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import software.wings.beans.ce.CECluster;
import software.wings.beans.ce.CECluster.CEClusterKeys;

import java.util.List;

public class CEClusterDao {
  private final HPersistence hPersistence;

  @Inject
  public CEClusterDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public boolean create(CECluster ceCluster) {
    return hPersistence.save(ceCluster) != null;
  }

  public List<CECluster> getByInfraAccountId(String accountId, String infraAccountId) {
    return hPersistence.createQuery(CECluster.class)
        .field(CEClusterKeys.accountId)
        .equal(accountId)
        .field(CEClusterKeys.infraAccountId)
        .equal(infraAccountId)
        .asList();
  }

  public boolean deleteCluster(String uuid) {
    Query<CECluster> query = hPersistence.createQuery(CECluster.class).field(CEClusterKeys.uuid).equal(uuid);
    return hPersistence.delete(query);
  }
}
