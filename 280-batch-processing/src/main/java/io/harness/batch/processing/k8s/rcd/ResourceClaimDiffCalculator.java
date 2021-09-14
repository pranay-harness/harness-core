/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.k8s.rcd;

/**
 * Calculates diff in ResourceClaim due to a change for a workload based on old & new yaml.
 */
public interface ResourceClaimDiffCalculator {
  String getKind();

  ResourceClaimDiff computeResourceClaimDiff(String oldYaml, String newYaml);
}
