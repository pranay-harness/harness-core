package io.harness.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateHeartbeatResponse {
  String delegateId;
  String status;
  boolean useCdn;
  String jreVersion;
  String delegateRandomToken;
  String sequenceNumber;
}
