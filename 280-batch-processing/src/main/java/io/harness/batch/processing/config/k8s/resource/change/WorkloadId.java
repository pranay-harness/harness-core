package io.harness.batch.processing.config.k8s.resource.change;

import lombok.Value;

@Value(staticConstructor = "of")
public class WorkloadId {
  String clusterId;
  String uid;
}
