package io.harness.ccm.setup.dao;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import software.wings.beans.ce.CECluster;

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
