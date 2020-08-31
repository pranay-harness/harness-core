package io.harness.batch.processing.config.k8s.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceId {
  static final ResourceId NOT_FOUND = ResourceId.builder().build();

  String accountId;
  String clusterId;
  String namespace;
  String name;
  String kind;
}
