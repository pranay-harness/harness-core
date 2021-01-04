package software.wings.helpers.ext.nexus.model;

import lombok.Builder;

@lombok.Data
@Builder
public class Nexus3RequestData {
  private String repositoryName;
  private String node;
}
