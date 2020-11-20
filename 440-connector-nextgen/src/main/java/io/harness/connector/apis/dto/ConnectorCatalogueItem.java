package io.harness.connector.apis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.swagger.annotations.ApiModel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

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
