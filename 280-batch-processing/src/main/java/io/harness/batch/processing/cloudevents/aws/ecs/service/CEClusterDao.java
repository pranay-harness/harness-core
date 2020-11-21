package io.harness.batch.processing.cloudevents.aws.ecs.service;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.persistence.HPersistence;

import software.wings.beans.ce.CECluster;
import software.wings.beans.ce.CECluster.CEClusterKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
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

  public List<CECluster> getCECluster(String accountId) {
    return hPersistence.createQuery(CECluster.class).field(CEClusterKeys.accountId).equal(accountId).asList();
  }

  public boolean deleteCluster(String uuid) {
    Query<CECluster> query =
        hPersistence.createQuery(CECluster.class, excludeAuthority).field(CEClusterKeys.uuid).equal(uuid);
    return hPersistence.delete(query);
  }
}
