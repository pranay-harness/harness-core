package io.harness.connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface ManagerExecutable {
  Boolean getExecuteOnManager();
  void setExecuteOnManager(Boolean setExecuteOnManager);
}
