package io.harness.perpetualtask.k8s.watch.functions;

import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.perpetualtask.k8s.watch.Owner;
import io.harness.perpetualtask.k8s.watch.Owner.Builder;

import java.util.Optional;
import javax.validation.constraints.NotNull;

public class JobOwnerMappingFunction
    implements PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner> {
  @Override
  public Owner apply(
      @NotNull OwnerReference ownerReference, @NotNull KubernetesClient kubernetesClient, @NotNull String namespace) {
    Optional<Job> job = kubernetesClient.extensions()
                            .jobs()
                            .inNamespace(namespace)
                            .list()
                            .getItems()
                            .stream()
                            .filter(j -> j != null && j.getMetadata().getName().equals(ownerReference.getName()))
                            .findFirst();
    Builder ownerBuilder = Owner.newBuilder();
    if (job.isPresent()) {
      Optional<OwnerReference> parentOwnerReference =
          job.get().getMetadata().getOwnerReferences().stream().filter(or -> or != null).findFirst();

      if (parentOwnerReference.isPresent()) {
        ownerBuilder.setUid(parentOwnerReference.get().getUid())
            .setName(parentOwnerReference.get().getName())
            .setKind(parentOwnerReference.get().getKind());
      }
    }
    return ownerBuilder.build();
  }
}
