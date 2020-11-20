
package io.harness.connector.apis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("ConnectorCatalogueResponse")
public class ConnectorCatalogueResponseDTO {
  List<ConnectorCatalogueItem> catalogue;
}
