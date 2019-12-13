package io.harness.perpetualtask.k8s.watch.functions;

import static io.harness.perpetualtask.k8s.watch.functions.OwnerMappingUtils.getOwnerFromParent;

import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.k8s.model.Kind;
import io.harness.perpetualtask.k8s.watch.Owner;

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
                            .filter(item -> ownerReference.getName().equals(item.getMetadata().getName()))
                            .findFirst();

    Owner owner = null;
    if (job.isPresent()) {
      owner = getOwnerFromParent(job.get());
    }

    return owner == null ? Owner.newBuilder()
                               .setName(ownerReference.getName())
                               .setKind(Kind.Job.name())
                               .setUid(ownerReference.getUid())
                               .build()
                         : owner;
  }
}
