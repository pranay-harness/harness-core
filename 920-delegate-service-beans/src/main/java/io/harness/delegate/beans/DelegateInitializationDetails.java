/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateInitializationDetails {
  private String delegateId;
  private String hostname;
  private boolean initialized;
  private boolean profileError;
  private long profileExecutedAt;
}
