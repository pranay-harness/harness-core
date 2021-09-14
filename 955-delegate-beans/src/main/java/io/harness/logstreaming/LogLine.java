/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import io.harness.logging.LogLevel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogLine {
  @JsonProperty("level") @NotNull LogLevel level;

  @JsonProperty("out") @NotNull String message;

  @JsonProperty("time") @NotNull Instant timestamp;

  @JsonProperty("pos") int position;

  @JsonProperty("args") Map<String, String> arguments;
}
