/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.threading;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class ThreadPoolConfig {
  @JsonProperty(defaultValue = "1") @Builder.Default int corePoolSize = 1;
  @JsonProperty(defaultValue = "5") @Builder.Default int maxPoolSize = 5;
  @JsonProperty(defaultValue = "30") @Builder.Default long idleTime = 30;
  @JsonProperty(defaultValue = "SECONDS") @Builder.Default TimeUnit timeUnit = TimeUnit.SECONDS;
}
