package io.harness.delegate.beans.logstreaming;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.logging.LogLevel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class LogLine {
  @JsonProperty("level") @NotNull LogLevel level;

  @JsonProperty("out") @NotNull String message;

  @JsonProperty("time") @NotNull Instant timestamp;

  @JsonProperty("pos") int position;

  @JsonProperty("args") Map<String, String> arguments;
}
