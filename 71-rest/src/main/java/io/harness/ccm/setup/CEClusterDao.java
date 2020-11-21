package io.harness.ccm.setup;

import io.harness.persistence.HPersistence;

import software.wings.beans.ce.CECluster;

import com.google.inject.Inject;

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
