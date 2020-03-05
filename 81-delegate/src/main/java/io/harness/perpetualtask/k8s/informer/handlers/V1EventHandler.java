package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.openapi.models.V1Event;

public class V1EventHandler extends BaseHandler<V1Event> {
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
