package io.harness.cvng.beans.newrelic;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.stackdriver.StackdriverDashboardRequest;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("NEWRELIC_APPS_REQUEST")
@SuperBuilder
@NoArgsConstructor
public class NewRelicApplicationFetchRequest extends DataCollectionRequest<NewRelicConnectorDTO> {
  public static final String DSL = StackdriverDashboardRequest.readDSL(
      "newrelic-applications.datacollection", NewRelicApplicationFetchRequest.class);

  @Override
  public String getDSL() {
    return DSL;
  }

  @Override
  public String getBaseUrl() {
    return NewRelicUtils.getBaseUrl(getConnectorConfigDTO());
  }

  @Override
  public Map<String, String> collectionHeaders() {
    return NewRelicUtils.collectionHeaders(getConnectorConfigDTO());
  }
}
