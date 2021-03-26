package io.harness.cvng.beans;

import io.harness.cvng.beans.appd.AppDynamicsUtils;
import io.harness.cvng.beans.newrelic.NewRelicUtils;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewRelicDataCollectionInfo extends TimeSeriesDataCollectionInfo<NewRelicConnectorDTO> {
  private String applicationName;
  private long applicationId;
  private MetricPackDTO metricPack;

  @Override
  public Map<String, Object> getDslEnvVariables(NewRelicConnectorDTO newRelicConnectorDTO) {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("appName", getApplicationName());
    dslEnvVariables.put("appId", getApplicationId());
    Map<String, String> queryPathMap = getQueryToPathMap();
    dslEnvVariables.put("queries", new ArrayList<>(queryPathMap.keySet()));
    dslEnvVariables.put("jsonPaths", new ArrayList<>(queryPathMap.values()));
    dslEnvVariables.put("metricNames",
        metricPack.getMetrics().stream().map(MetricPackDTO.MetricDefinitionDTO::getName).collect(Collectors.toList()));
    dslEnvVariables.put("collectHostData", Boolean.toString(this.isCollectHostData()));
    return dslEnvVariables;
  }

  @Override
  public String getBaseUrl(NewRelicConnectorDTO newRelicConnectorDTO) {
    return NewRelicUtils.getBaseUrl(newRelicConnectorDTO);
  }

  @Override
  public Map<String, String> collectionHeaders(NewRelicConnectorDTO newRelicConnectorDTO) {
    return NewRelicUtils.collectionHeaders(newRelicConnectorDTO);
  }

  @Override
  public Map<String, String> collectionParams(NewRelicConnectorDTO newRelicConnectorDTO) {
    return Collections.emptyMap();
  }

  private Map<String, String> getQueryToPathMap() {
    Map<String, String> returnMap = new HashMap<>();
    metricPack.getMetrics().forEach(metricDefinitionDTO -> {
      returnMap.put(metricDefinitionDTO.getPath(), metricDefinitionDTO.getResponseJsonPath());
    });
    return returnMap;
  }
}
