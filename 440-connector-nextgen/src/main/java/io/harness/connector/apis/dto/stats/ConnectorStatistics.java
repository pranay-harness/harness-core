package io.harness.connector.apis.dto.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Value
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "ConnectorStatisticsKeys")
@Builder
@ApiModel("ConnectorStatistics")
public class ConnectorStatistics {
  List<ConnectorTypeStats> typeStats;
  List<ConnectorStatusStats> statusStats;
}
