package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.kubernetes.client.openapi.models.V1Job;

public class V1JobHandler extends BaseHandler<V1Job> {
  public V1JobHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Job";
  }

  @Override
  String getApiVersion() {
    return "batch/v1";
  }
}
