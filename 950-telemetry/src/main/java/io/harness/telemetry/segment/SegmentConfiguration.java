/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.segment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;
import io.harness.telemetry.TelemetryConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentConfiguration implements TelemetryConfiguration {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String url;
  @ConfigSecret private String apiKey;
  private boolean certValidationRequired;
}
