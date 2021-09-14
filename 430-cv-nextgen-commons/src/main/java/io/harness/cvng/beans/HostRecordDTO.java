/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class HostRecordDTO {
  String accountId;
  String verificationTaskId;
  Set<String> hosts;
  Instant startTime;
  Instant endTime;
}
