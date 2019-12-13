package io.harness.perpetualtask.k8s.watch.functions;

@FunctionalInterface
public interface PodOwnerMappingFunction<R, K, N, O> {
  O apply(R r, K k, N n);
}