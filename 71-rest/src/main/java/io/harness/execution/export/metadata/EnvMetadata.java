package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.EnvSummary;
import software.wings.beans.Environment.EnvironmentType;

import java.util.List;

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
