
package io.harness.connector;

import io.harness.ConnectorConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
@ApiModel("ConnectorCatalogueResponse")
@Schema(name = "ConnectorCatalogueResponse", description = "This has details of the retrieved Connector Catalogue.")
public class ConnectorCatalogueResponseDTO {
  @Schema(description = ConnectorConstants.CONNECTOR_CATALOGUE_LIST) List<ConnectorCatalogueItem> catalogue;
}
