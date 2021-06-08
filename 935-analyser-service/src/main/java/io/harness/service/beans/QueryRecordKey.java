package io.harness.service.beans;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class QueryRecordKey {
  @NonNull String hash;
  @NonNull String version;
  @NonNull String serviceName;
}
