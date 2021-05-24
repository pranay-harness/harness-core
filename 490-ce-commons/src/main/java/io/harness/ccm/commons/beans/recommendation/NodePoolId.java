package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class NodePoolId {
  // nodepoolname can be null for some nodes
  String nodepoolname;
  @NonNull String clusterid;
}
