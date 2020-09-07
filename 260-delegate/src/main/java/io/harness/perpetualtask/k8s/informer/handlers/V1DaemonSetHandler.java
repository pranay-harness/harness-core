package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1DaemonSet;

import java.util.List;

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

  @Override
  protected void publishWorkloadSpecOnAdd(V1DaemonSet daemonSet, Timestamp occurredAt) {
    if (daemonSet.getMetadata() != null && daemonSet.getSpec() != null
        && daemonSet.getSpec().getTemplate().getSpec() != null) {
      List<V1Container> containers = daemonSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = daemonSet.getSpec().getTemplate().getSpec().getInitContainers();
      publishWorkloadSpec(K8sWorkloadSpec.newBuilder()
                              .setWorkloadKind(getKind())
                              .setWorkloadName(daemonSet.getMetadata().getName())
                              .setNamespace(daemonSet.getMetadata().getNamespace())
                              .addAllContainerSpecs(makeContainerSpecs(containers))
                              .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                              .build(),
          occurredAt);
    }
  }

  @Override
  protected void publishWorkloadSpecIfChangedOnUpdate(
      V1DaemonSet oldDaemonSet, V1DaemonSet newDaemonSet, Timestamp occurredAt) {
    if (oldDaemonSet.getSpec() != null && newDaemonSet.getSpec() != null
        && oldDaemonSet.getSpec().getTemplate().getSpec() != null
        && newDaemonSet.getSpec().getTemplate().getSpec() != null && oldDaemonSet.getMetadata() != null
        && newDaemonSet.getMetadata() != null) {
      List<V1Container> containers = oldDaemonSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = oldDaemonSet.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec oldSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(oldDaemonSet.getMetadata().getName())
                                     .setNamespace(oldDaemonSet.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .build();
      List<V1Container> newContainers = newDaemonSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers = newDaemonSet.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newDaemonSet.getMetadata().getName())
                                     .setNamespace(newDaemonSet.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(newContainers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(newInitContainers))
                                     .build();
      if (!oldSpecs.equals(newSpecs)) {
        publishWorkloadSpec(newSpecs, occurredAt);
      }
    }
  }
}
