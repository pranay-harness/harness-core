package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.openapi.models.V1DaemonSet;

public class V1DaemonSetHandler extends BaseHandler<V1DaemonSet> {
  public V1DaemonSetHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "DaemonSet";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }
}
