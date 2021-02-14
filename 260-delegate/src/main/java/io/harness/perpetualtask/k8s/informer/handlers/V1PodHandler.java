package io.harness.perpetualtask.k8s.informer.handlers;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;

import io.kubernetes.client.openapi.models.V1Pod;

@TargetModule(Module._420_DELEGATE_AGENT)
public class V1PodHandler extends BaseHandler<V1Pod> {
  public V1PodHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "Pod";
  }

  @Override
  String getApiVersion() {
    return "v1";
  }
}
