package io.harness.util;

import lombok.Builder;

import java.util.Set;
import javax.validation.constraints.NotNull;

@Builder
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
