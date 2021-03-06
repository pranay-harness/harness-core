package io.harness.aggregator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AggregatorConfiguration {
  @JsonProperty("debezium") private DebeziumConfig debeziumConfig;
  private boolean enabled;
}
