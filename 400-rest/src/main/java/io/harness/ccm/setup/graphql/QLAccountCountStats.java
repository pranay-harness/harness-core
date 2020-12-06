package io.harness.ccm.setup.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLAccountCountStats {
  Integer countOfConnected;
  Integer countOfNotConnected;
  Integer countOfNotVerified;
}
