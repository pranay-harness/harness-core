/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
