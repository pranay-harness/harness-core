package io.harness.delegate.beans.cvng.newrelic;

import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewRelicConnectorValidationInfo extends ConnectorValidationInfo<NewRelicConnectorDTO> {
  private static final String BASE_URL = "v1/accounts/";
  private static final String DSL =
      readDSL("newrelic-validation.datacollection", NewRelicConnectorValidationInfo.class);
  @Override
  public String getConnectionValidationDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return getConnectorConfigDTO().getUrl() + BASE_URL + getConnectorConfigDTO().getNewRelicAccountId() + "/";
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return NewRelicUtils.collectionHeaders(getConnectorConfigDTO());
  }
}