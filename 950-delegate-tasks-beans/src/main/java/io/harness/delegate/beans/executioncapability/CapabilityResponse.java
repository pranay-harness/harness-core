/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
