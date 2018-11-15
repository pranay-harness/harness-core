package software.wings.service.impl;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerMetadata {
  private String containerServiceName;
  private String namespace;
}
