package io.harness.perpetualtask.k8s.watch.functions;

import static io.harness.perpetualtask.k8s.watch.functions.OwnerMappingUtils.getOwnerFromParent;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.extensions.DoneableReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.harness.perpetualtask.k8s.watch.Owner;

import javax.validation.constraints.NotNull;

public class ReplicaSetOwnerMappingFunction
    implements PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner> {
  @Override
  public Owner apply(
      @NotNull OwnerReference ownerReference, @NotNull KubernetesClient kubernetesClient, @NotNull String namespace) {
    ScalableResource<ReplicaSet, DoneableReplicaSet> replicaSetResource =
        kubernetesClient.extensions().replicaSets().inNamespace(namespace).withName(ownerReference.getName());

    if (replicaSetResource != null) {
      ReplicaSet replicaSet = replicaSetResource.get();
      if (replicaSet != null) {
        return getOwnerFromParent(replicaSet);
      }
    }

    return Owner.newBuilder()
        .setUid(ownerReference.getUid())
        .setName(ownerReference.getName())
        .setKind(ownerReference.getKind())
        .build();
  }
}
