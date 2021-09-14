/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogStreamingServiceConfiguration {
  private String baseUrl;
  private String serviceToken;
}
