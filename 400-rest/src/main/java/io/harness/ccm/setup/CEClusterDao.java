package io.harness.ccm.setup;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import software.wings.beans.ce.CECluster;

import com.google.inject.Inject;

@OwnedBy(CE)
public class CEClusterDao {
  private final HPersistence hPersistence;

  @Inject
  public CEClusterDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public boolean create(CECluster ceCluster) {
    return hPersistence.save(ceCluster) != null;
  }
}
