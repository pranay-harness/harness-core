package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;

import io.kubernetes.client.openapi.models.CoreV1Event;

@OwnedBy(HarnessTeam.CE)
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class V1EventHandler extends BaseHandler<CoreV1Event> {
  public V1EventHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Event";
  }

  @Override
  String getApiVersion() {
    return "v1";
  }
}
