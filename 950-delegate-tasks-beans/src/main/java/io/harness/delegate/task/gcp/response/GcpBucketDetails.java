package io.harness.delegate.task.gcp.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class GcpBucketDetails {
  String id;
  String name;
}
