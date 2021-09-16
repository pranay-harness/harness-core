package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public class HttpBuildStoreTypeSpec implements BuildStoreTypeSpec {
  String connectorRef;

  @Override
  public String fetchConnectorRef() {
    return connectorRef;
  }
}
