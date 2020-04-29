package io.harness.batch.processing.k8s.rcd;

/**
 * Calculates diff in ResourceClaim due to a change for a workload based on old & new yaml.
 */
public interface ResourceClaimDiffCalculator {
  String getKind();
  ResourceClaim computeResourceDiff(String oldYaml, String newYaml);
}
