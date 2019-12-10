package io.harness.perpetualtask.k8s.watch.functions;

@FunctionalInterface
public interface PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, Owner> {
  Owner apply(OwnerReference ownerReference, KubernetesClient kubernetesClient, String namespace);
}
