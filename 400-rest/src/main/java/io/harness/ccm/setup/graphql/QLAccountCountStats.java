package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class QLAccountCountStats {
  Integer countOfConnected;
  Integer countOfNotConnected;
  Integer countOfNotVerified;
}
