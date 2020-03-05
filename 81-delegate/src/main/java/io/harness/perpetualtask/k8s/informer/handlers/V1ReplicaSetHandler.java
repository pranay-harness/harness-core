package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.openapi.models.V1ReplicaSet;

public class V1ReplicaSetHandler extends BaseHandler<V1ReplicaSet> {
  public V1ReplicaSetHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "ReplicaSet";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }
}
