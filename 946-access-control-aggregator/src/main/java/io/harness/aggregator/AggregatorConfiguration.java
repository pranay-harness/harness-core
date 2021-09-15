/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.aggregator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class AggregatorConfiguration {
  @JsonProperty("debezium") private DebeziumConfig debeziumConfig;
  private boolean enabled;
  private boolean exportMetricsToStackDriver;
  public static final String ACCESS_CONTROL_SERVICE = "ACCESS_CONTROL_SERVICE";
}
