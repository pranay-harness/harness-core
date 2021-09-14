/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;

import software.wings.beans.EnvSummary;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class EnvMetadata {
  String name;
  EnvironmentType environmentType;

  static EnvMetadata fromFirstEnvSummary(List<EnvSummary> envSummaries) {
    if (isEmpty(envSummaries)) {
      return null;
    }

    return fromEnvSummary(envSummaries.get(0));
  }

  private static EnvMetadata fromEnvSummary(EnvSummary envSummary) {
    if (envSummary == null || (envSummary.getName() == null && envSummary.getEnvironmentType() == null)) {
      return null;
    }

    return EnvMetadata.builder().name(envSummary.getName()).environmentType(envSummary.getEnvironmentType()).build();
  }
}
