package io.harness.ngtriggers.beans.source.artifact.store;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.artifact.BuildStoreTypeSpec;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class GcsBuildStoreTypeSpec implements BuildStoreTypeSpec {
  String connectorRef;
  String bucketName;
  String folderPath;

  @Override
  public String fetchConnectorRef() {
    return connectorRef;
  }
}
