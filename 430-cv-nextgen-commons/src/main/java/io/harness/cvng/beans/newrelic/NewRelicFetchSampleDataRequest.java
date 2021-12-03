package io.harness.cvng.beans.newrelic;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.cvng.newrelic.NewRelicUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("NEWRELIC_SAMPLE_FETCH_REQUEST")
@Data
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class NewRelicFetchSampleDataRequest extends DataCollectionRequest<NewRelicConnectorDTO> {
  public static final String DSL =
      DataCollectionRequest.readDSL("newrelic-sample-fetch.datacollection", NewRelicFetchSampleDataRequest.class);

  String query;
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

  @Override
  public Map<String, Object> fetchDslEnvVariables() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("query", query);
    return variables;
  }
}
