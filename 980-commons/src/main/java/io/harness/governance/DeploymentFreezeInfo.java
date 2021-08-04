package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class DeploymentFreezeInfo {
  private boolean freezeAll; // This is set to true if the master deployment freeze is on
  private Set<String> allEnvFrozenApps; // Applications for which all envs are frozen
  private Map<String, Set<String>> appEnvs; // This has a list of applications with the frozen environments in it
                                            // excluding the apps from previous list
}
