package io.harness.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.alert.Alert;

@OwnedBy(HarnessTeam.DEL)
public interface DmsProxy {
  void alertCheckJobExecute(String accountId);

  boolean alertReconCanAssign(Alert alert);
}
