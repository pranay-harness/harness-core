package io.harness.ngtriggers.beans.entity.metadata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArtifactMetadata {
  String connectorRef;
  String imagePath;
  String path;
}
