package io.harness.ngtriggers.beans.source.artifact.store;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.artifact.BuildStoreTypeSpec;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class HttpBuildStoreTypeSpec implements BuildStoreTypeSpec {
  String connectorRef;

  @Override
  public String fetchConnectorRef() {
    return connectorRef;
  }
}
