package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrometheusDataCollectionInfo extends TimeSeriesDataCollectionInfo<PrometheusConnectorDTO> {
  private String groupName;
  private List<MetricCollectionInfo> metricCollectionInfoList;

  @Data
  @Builder
  public static class MetricCollectionInfo {
    private String query;
    private String metricName;
    private String filters;
  }
  @Override
  public Map<String, Object> getDslEnvVariables(PrometheusConnectorDTO connectorConfigDTO) {
    Map<String, Object> collectionEnvs = new HashMap<>();
    List<String> queryList = new ArrayList<>();
    List<String> metricNameList = new ArrayList<>();
    metricCollectionInfoList.forEach(metricCollectionInfo -> {
      queryList.add(metricCollectionInfo.getQuery());
      metricNameList.add(metricCollectionInfo.getMetricName());
    });
    Preconditions.checkState(queryList.size() == metricNameList.size());
    collectionEnvs.put("queryList", queryList);
    collectionEnvs.put("metricNameList", metricNameList);
    collectionEnvs.put("groupName", groupName);
    return collectionEnvs;
  }

  @Override
  public String getBaseUrl(PrometheusConnectorDTO connectorConfigDTO) {
    return connectorConfigDTO.getUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(PrometheusConnectorDTO connectorConfigDTO) {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, String> collectionParams(PrometheusConnectorDTO connectorConfigDTO) {
    return Collections.emptyMap();
  }
}
