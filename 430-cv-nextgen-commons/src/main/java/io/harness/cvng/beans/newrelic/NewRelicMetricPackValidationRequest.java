package io.harness.cvng.beans.newrelic;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("NEWRELIC_VALIDATION_REQUEST")
@Data
@SuperBuilder
@NoArgsConstructor
public class NewRelicMetricPackValidationRequest extends DataCollectionRequest<NewRelicConnectorDTO> {
  public static final String DSL = DataCollectionRequest.readDSL(
      "newrelic-metric-pack-validation.datacollection", NewRelicMetricPackValidationRequest.class);

  private String applicationName;
  private String applicationId;
  private Set<MetricPackDTO> metricPackDTOSet;

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
    Map<String, Object> envVariables = new HashMap<>();
    envVariables.put("appId", applicationId);
    envVariables.put("appName", applicationName);
    Map<String, String> queryPathMap = getQueryToPathMap();
    envVariables.put("queries", new ArrayList<>(queryPathMap.keySet()));
    envVariables.put("jsonPaths", new ArrayList<>(queryPathMap.values()));
    envVariables.put("metricNames", getMetricNames());
    return envVariables;
  }

  private List<String> getMetricNames() {
    if (metricPackDTOSet != null) {
      List<String> metricNames = new ArrayList<>();
      metricPackDTOSet.forEach(metricPackDTO -> {
        metricPackDTO.getMetrics().forEach(metricDefinitionDTO -> { metricNames.add(metricDefinitionDTO.getName()); });
      });
      return metricNames;
    }
    return null;
  }

  private Map<String, String> getQueryToPathMap() {
    if (metricPackDTOSet != null) {
      Map<String, String> returnMap = new HashMap<>();
      metricPackDTOSet.forEach(metricPackDTO -> {
        metricPackDTO.getMetrics().forEach(metricDefinitionDTO -> {
          returnMap.put(metricDefinitionDTO.getValidationPath(), metricDefinitionDTO.getResponseJsonPath());
        });
      });
      return returnMap;
    }
    return null;
  }
}