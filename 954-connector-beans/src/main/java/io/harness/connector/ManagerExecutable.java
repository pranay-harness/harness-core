package io.harness.connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface ManagerExecutable {
  Boolean getExecuteOnDelegate();
  void setExecuteOnDelegate(Boolean executeOnDelegate);
}
