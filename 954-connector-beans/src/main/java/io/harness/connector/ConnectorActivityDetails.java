package io.harness.connector;

import io.harness.ConnectorConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ConnectorActivityDetailsKeys")
@Schema(description = ConnectorConstants.CONNECTOR_ACTIVITY_DETAILS)
public class ConnectorActivityDetails {
  @Schema(description = ConnectorConstants.ACTIVITY_TIME) Long lastActivityTime;
}
