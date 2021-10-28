package io.harness.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.alert.Alert;

@OwnedBy(HarnessTeam.DEL)
public class DmsProxyDelegateModeImpl implements DmsProxy {
  @Override
  public void alertCheckJobExecute(String accountId) {
    // make grpc
  }

  @Override
  public boolean alertReconCanAssign(Alert alert) {
    // make grpc
    // make alert object kryo serializable.
    return false;
  }
}
