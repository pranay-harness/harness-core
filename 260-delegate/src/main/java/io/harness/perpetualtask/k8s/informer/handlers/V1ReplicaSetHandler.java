package io.harness.perpetualtask.k8s.informer.handlers;

import static io.harness.perpetualtask.k8s.informer.handlers.support.WorkloadSpecUtils.makeContainerSpecs;

import com.google.protobuf.Timestamp;

import io.harness.event.client.EventPublisher;
import io.harness.perpetualtask.k8s.informer.ClusterDetails;
import io.harness.perpetualtask.k8s.watch.K8sWorkloadSpec;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ReplicaSet;

import java.util.List;

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

  @Override
  protected void publishWorkloadSpecOnAdd(V1ReplicaSet replicaSet, Timestamp occurredAt) {
    if (replicaSet.getMetadata() != null && replicaSet.getSpec() != null && replicaSet.getSpec().getTemplate() != null
        && replicaSet.getSpec().getTemplate().getSpec() != null) {
      List<V1Container> containers = replicaSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = replicaSet.getSpec().getTemplate().getSpec().getInitContainers();
      publishWorkloadSpec(K8sWorkloadSpec.newBuilder()
                              .setWorkloadKind(getKind())
                              .setWorkloadName(replicaSet.getMetadata().getName())
                              .setNamespace(replicaSet.getMetadata().getNamespace())
                              .addAllContainerSpecs(makeContainerSpecs(containers))
                              .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                              .build(),
          occurredAt);
    }
  }

  @Override
  protected void publishWorkloadSpecIfChangedOnUpdate(
      V1ReplicaSet oldReplicaSet, V1ReplicaSet newReplicaSet, Timestamp occurredAt) {
    if (oldReplicaSet.getSpec() != null && newReplicaSet.getSpec() != null
        && oldReplicaSet.getSpec().getTemplate() != null && newReplicaSet.getSpec().getTemplate() != null
        && oldReplicaSet.getSpec().getTemplate().getSpec() != null
        && newReplicaSet.getSpec().getTemplate().getSpec() != null && oldReplicaSet.getMetadata() != null
        && newReplicaSet.getMetadata() != null) {
      List<V1Container> containers = oldReplicaSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> initContainers = oldReplicaSet.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec oldSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(oldReplicaSet.getMetadata().getName())
                                     .setNamespace(oldReplicaSet.getMetadata().getNamespace())
                                     .setWorkloadKind(getKind())
                                     .addAllContainerSpecs(makeContainerSpecs(containers))
                                     .addAllInitContainerSpecs(makeContainerSpecs(initContainers))
                                     .build();
      List<V1Container> newContainers = newReplicaSet.getSpec().getTemplate().getSpec().getContainers();
      List<V1Container> newInitContainers = newReplicaSet.getSpec().getTemplate().getSpec().getInitContainers();
      K8sWorkloadSpec newSpecs = K8sWorkloadSpec.newBuilder()
                                     .setWorkloadKind(getKind())
                                     .setWorkloadName(newReplicaSet.getMetadata().getName())
                                     .setNamespace(newReplicaSet.getMetadata().getNamespace())
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
