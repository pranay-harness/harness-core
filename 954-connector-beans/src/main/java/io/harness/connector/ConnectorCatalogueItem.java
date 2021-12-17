package io.harness.connector;

import io.harness.ConnectorConstants;
import io.harness.delegate.beans.connector.ConnectorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("ConnectorCatalogueItem")
@Schema(description = "This has details of the Connector Catalogue in Harness.")
public class ConnectorCatalogueItem {
  @Schema(description = ConnectorConstants.CONNECTOR_CATEGORY) ConnectorCategory category;
  @Schema(description = ConnectorConstants.CONNECTOR_TYPE_LIST_BY_CATEGORY) Set<ConnectorType> connectors;
}
