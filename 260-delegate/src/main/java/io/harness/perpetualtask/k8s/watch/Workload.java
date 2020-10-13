package io.harness.perpetualtask.k8s.watch;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Value;

@Value(staticConstructor = "of")
public class Workload {
  String kind;
  V1ObjectMeta objectMeta;
}
