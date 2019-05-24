package io.harness.influx;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfluxConfig {
  String influxUri;
  String influxUserName;
  String influxPassword;
  String influxDatabase;
}
