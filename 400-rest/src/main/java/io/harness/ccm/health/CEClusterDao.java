package io.harness.ccm.health;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.HPersistence;

import software.wings.beans.ce.CECluster;
import software.wings.beans.ce.CECluster.CEClusterKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._490_CE_COMMONS)
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
