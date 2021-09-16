package io.harness.service.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class QueryRecordKey {
  @NonNull String hash;
  @NonNull String version;
  @NonNull String serviceName;
}
