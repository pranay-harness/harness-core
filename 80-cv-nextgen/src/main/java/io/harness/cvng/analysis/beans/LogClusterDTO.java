package io.harness.cvng.analysis.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogClusterDTO {
  String cvConfigId;
  long epochMinute;
  String host;
  String log;
  String clusterLabel;
  int clusterCount;
}
