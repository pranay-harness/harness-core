/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
