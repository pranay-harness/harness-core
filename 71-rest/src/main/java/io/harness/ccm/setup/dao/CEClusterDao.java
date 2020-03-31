package io.harness.ccm.setup.dao;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;

public class CEClusterDao {
  private final HPersistence hPersistence;

  @Inject
  public CEClusterDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }
}
