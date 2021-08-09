package software.wings.service.impl;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerMetadata {
  private ContainerMetadataType type;
  private String containerServiceName;
  private String namespace;
  private String releaseName;
}
