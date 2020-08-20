package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AppDynamicsDataCollectionInfo extends TimeSeriesDataCollectionInfo<AppDynamicsConnectorDTO> {
  private long tierId;
  private long applicationId;
  private MetricPackDTO metricPack;
  @Override
  public Map<String, Object> getDslEnvVariables() {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("appId", getApplicationId());
    dslEnvVariables.put("tierId", getTierId());
    final List<String> metricPaths = getMetricPack()
                                         .getMetrics()
                                         .stream()
                                         .filter(metricDefinition -> metricDefinition.isIncluded())
                                         .map(metricDefinition -> metricDefinition.getPath())
                                         .collect(Collectors.toList());
    dslEnvVariables.put("metricsToCollect", metricPaths);
    return dslEnvVariables;
  }

  @Override
  public String getBaseUrl(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return appDynamicsConnectorDTO.getControllerUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", getHeaderWithCredentials(appDynamicsConnectorDTO));
    return headers;
  }

  @Override
  public Map<String, String> collectionParams(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return Collections.emptyMap();
  }

  private String getHeaderWithCredentials(AppDynamicsConnectorDTO appDynamicsConnectorDTO) {
    return "Basic "
        + Base64.encodeBase64String(
              String
                  .format("%s@%s:%s", appDynamicsConnectorDTO.getUsername(), appDynamicsConnectorDTO.getAccountname(),
                      new String(appDynamicsConnectorDTO.getPasswordRef().getDecryptedValue()))
                  .getBytes(StandardCharsets.UTF_8));
  }
}
