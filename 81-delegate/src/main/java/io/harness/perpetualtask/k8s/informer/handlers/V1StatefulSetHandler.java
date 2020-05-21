package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1StatefulSet;

import java.util.List;

public class V1StatefulSetHandler extends BaseHandler<V1StatefulSet> {
  public V1StatefulSetHandler(EventPublisher eventPublisher, ClusterDetails clusterDetails) {
    super(eventPublisher, clusterDetails);
  }

  @Override
  String getKind() {
    return "StatefulSet";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }

  @Override
  protected void publishWorkloadSpecOnAdd(V1StatefulSet statefulSet, Timestamp occurredAt) {
    if (statefulSet.getMetadata() != null && statefulSet.getSpec() != null
        && statefulSet.getSpec().getTemplate().getSpec() != null) {
      List<V1Container> containers = statefulSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = statefulSet.getSpec().getTemplate().getSpec().getInitContainers();
      publishWorkloadSpec(K8sWorkloadSpec.newBuilder()
                              .setWorkloadKind(getKind())
                              .setWorkloadName(statefulSet.getMetadata().getName())
                              .setNamespace(statefulSet.getMetadata().getNamespace())
                              .addAllContainerSpecs(makeContainerSpecs(containers))
                              .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                              .build(),
          occurredAt);
    }
  }

  @Override
  protected void publishWorkloadSpecIfChangedOnUpdate(
      V1StatefulSet oldStatefulSet, V1StatefulSet newStatefulSet, Timestamp occurredAt) {
    if (oldStatefulSet.getSpec() != null && newStatefulSet.getSpec() != null
        && oldStatefulSet.getSpec().getTemplate().getSpec() != null
        && newStatefulSet.getSpec().getTemplate().getSpec() != null && oldStatefulSet.getMetadata() != null
        && newStatefulSet.getMetadata() != null) {
      List<V1Container> containers = oldStatefulSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = oldStatefulSet.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec oldSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(oldStatefulSet.getMetadata().getName())
                                     .setNamespace(oldStatefulSet.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .build();
      List<V1Container> newContainers = newStatefulSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers = newStatefulSet.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newStatefulSet.getMetadata().getName())
                                     .setNamespace(newStatefulSet.getMetadata().getNamespace())
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
