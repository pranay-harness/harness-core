/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cf;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class CfClientConfig {
  private String apiKey;
  @Default private String configUrl = "https://config.feature-flags.uat.harness.io/api/1.0";
  @Default private String eventUrl = "https://event.feature-flags.uat.harness.io/api/1.0";
  private boolean analyticsEnabled;
  @Default private int connectionTimeout = 10000;
  @Default private int readTimeout = 10000;
}
