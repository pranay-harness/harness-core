package io.harness.tracing.shapedetector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class QueryHashKey {
  String collectionName;
  String queryHash;
  String sortHash;
}
