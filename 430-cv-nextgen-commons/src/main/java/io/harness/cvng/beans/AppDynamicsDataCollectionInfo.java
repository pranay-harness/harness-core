package io.harness.cvng.beans;

import io.harness.cvng.beans.appd.AppDynamicsUtils;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

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
public class AppDynamicsDataCollectionInfo extends TimeSeriesDataCollectionInfo<AppDynamicsConnectorDTO> {
  private String applicationName;
  private String tierName;
  private MetricPackDTO metricPack;
  @Override
  public Map<String, Object> getDslEnvVariables(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("applicationName", getApplicationName());
    dslEnvVariables.put("tierName", getTierName());
    final List<String> metricPaths = getMetricPack()
                                         .getMetrics()
                                         .stream()
                                         .filter(metricDefinition -> metricDefinition.isIncluded())
                                         .map(metricDefinition -> metricDefinition.getPath())
                                         .collect(Collectors.toList());
    dslEnvVariables.put("metricsToCollect", metricPaths);
    dslEnvVariables.put("collectHostData", Boolean.toString(this.isCollectHostData()));
    return dslEnvVariables;
  }

  @Override
  public String getBaseUrl(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return appDynamicsConnectorDTO.getControllerUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", AppDynamicsUtils.getAuthorizationHeader(appDynamicsConnectorDTO));
    return headers;
  }

  @Override
  public Map<String, String> collectionParams(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return Collections.emptyMap();
  }
}
