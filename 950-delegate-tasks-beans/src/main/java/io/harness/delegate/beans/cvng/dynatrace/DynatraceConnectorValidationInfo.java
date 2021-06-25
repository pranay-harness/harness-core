package io.harness.delegate.beans.cvng.dynatrace;

import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DynatraceConnectorValidationInfo extends ConnectorValidationInfo<DynatraceConnectorDTO> {
  private static final String DSL =
      readDSL("dynatrace-validation.datacollection", DynatraceConnectorValidationInfo.class);

  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return connectorConfigDTO.getUrl();
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return DynatraceUtils.collectionHeaders(connectorConfigDTO);
  }
}
