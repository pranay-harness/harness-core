package io.harness.connector.apis.dto.stats;

import io.harness.connector.ConnectivityStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ConnectorStatusStatsKeys")
@Builder
@ApiModel("ConnectorStatusStatistics")
public class ConnectorStatusStats {
  ConnectivityStatus status;
  int count;
}
