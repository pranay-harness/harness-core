package io.harness.batch.processing.config.k8s.recommendation;

import lombok.Value;

@Value(staticConstructor = "of")
class ResourceId {
  static final ResourceId NOT_FOUND = of("", "", "", "", "");

  String accountId;
  String clusterId;
  String namespace;
  String name;
  String kind;
}
