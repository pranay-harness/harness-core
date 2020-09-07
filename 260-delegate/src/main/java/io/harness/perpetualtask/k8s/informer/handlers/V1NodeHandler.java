package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.openapi.models.V1Node;

public class V1NodeHandler extends BaseHandler<V1Node> {
  public V1NodeHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Node";
  }

  @Override
  String getApiVersion() {
    return "v1";
  }
}
