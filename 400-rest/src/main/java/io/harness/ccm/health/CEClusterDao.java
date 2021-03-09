package io.harness.ccm.health;

import com.google.inject.Inject;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ce.CECluster;
import software.wings.beans.ce.CECluster.CEClusterKeys;

import static io.harness.persistence.HQuery.excludeAuthority;

@Slf4j
public class CEClusterDao {
  private final HPersistence hPersistence;

  @Inject
  public CEClusterDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public CECluster getCECluster(String clusterId) {
    return hPersistence.createQuery(CECluster.class, excludeAuthority).field(CEClusterKeys.uuid).equal(clusterId).get();
  }
}
