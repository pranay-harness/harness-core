/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
