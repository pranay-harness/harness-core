package io.harness.util;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@OwnedBy(HarnessTeam.CI)
public class PortFinder {
  @NotNull private Set<Integer> usedPorts;
  @NotNull private Integer startingPort;

  public Integer getNextPort() {
    while (usedPorts.contains(startingPort)) {
      startingPort++;
    }
    return startingPort++;
  }
}
