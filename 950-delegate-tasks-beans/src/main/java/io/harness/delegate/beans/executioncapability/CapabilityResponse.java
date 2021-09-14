/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.executioncapability;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CapabilityResponse {
  private String accountId;
  private String delegateId;
  // This field (delegateCapability) wont be used right now, as we are not passing this response object back to manager.
  // As of now, we convert this into DelegateConnectionResult and send it back as manager understands it.
  // But going forward goal is make manager receive and understand this one.
  // This field contains entire structure of delegateCapability
  // (e.g. HttpConnectionExecutionCapability, would have hostName, port, scheme).
  private ExecutionCapability delegateCapability;
  private boolean validated;
}
