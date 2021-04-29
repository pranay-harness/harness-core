package io.harness.ccm.commons.beans.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceId {
  public static final ResourceId NOT_FOUND = ResourceId.builder().build();

  String accountId;
  String clusterId;
  String namespace;
  String name;
  String kind;
}
