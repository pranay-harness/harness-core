package io.harness.perpetualtask.k8s.watch;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Value;

@OwnedBy(HarnessTeam.CE)
@Value(staticConstructor = "of")
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class Workload {
  String kind;
  V1ObjectMeta objectMeta;
}
