package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DataCollectionConnectorBundle {
  // JSON serialization does not work for ConnectorConfigDTO without the wrapper so need to pass the whole object
  private ConnectorDTO connectorDTO;
  private Map<String, String> params;

  @JsonIgnore
  public ConnectorConfigDTO getConnectorConfigDTO() {
    return connectorDTO.getConnectorConfig();
  }
}
