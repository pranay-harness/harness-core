package io.harness.batch.processing.k8s.rcd;

import lombok.Value;

@Value
public class ResourceClaimDiff {
  ResourceClaim oldResourceClaim;
  ResourceClaim newResourceClaim;

  public ResourceClaim getDiff() {
    return newResourceClaim.minus(oldResourceClaim);
  }
}
