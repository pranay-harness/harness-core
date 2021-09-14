/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface HarnessAnnotations {
  String directApply = "harness.io/direct-apply";
  String skipVersioning = "harness.io/skip-versioning";
  String primaryService = "harness.io/primary-service";
  String stageService = "harness.io/stage-service";
  String managed = "harness.io/managed";
  String managedWorkload = "harness.io/managed-workload";
  String steadyStateCondition = "harness.io/steadyStateCondition";
  String skipPruning = "harness.io/skipPruning";
}
