package io.harness.connector.apis.dto;

import io.harness.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("ConnectorCatalogueItem")
public class ConnectorCatalogueItem {
  ConnectorCategory category;
  Set<ConnectorType> connectors;
}
