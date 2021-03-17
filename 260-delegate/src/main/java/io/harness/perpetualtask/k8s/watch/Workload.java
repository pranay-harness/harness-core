package io.harness.perpetualtask.k8s.watch;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Value;

@Value(staticConstructor = "of")
@TargetModule(Module._420_DELEGATE_AGENT)
public class Workload {
  String kind;
  V1ObjectMeta objectMeta;
}
