package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.delegate.beans.cvng.dynatrace.DynatraceUtils;

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
public class DynatraceDataCollectionInfo extends TimeSeriesDataCollectionInfo<DynatraceConnectorDTO> {
  private static final String METRIC_NAME_PARAM = "metricName";
  private static final String QUERY_SELECTOR_PARAM = "querySelector";

  private String groupName;
  private String serviceId;
  private List<MetricCollectionInfo> customMetrics;
  private MetricPackDTO metricPack;

  @Override
  public Map<String, Object> getDslEnvVariables(DynatraceConnectorDTO connectorConfigDTO) {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("entityId", serviceId);

    List<Map<String, String>> queryData = metricPack.getMetrics()
            .stream().map(metricDefinitionDTO -> {
              Map<String, String> metricMap = new HashMap<>();
              metricMap.put(METRIC_NAME_PARAM, metricDefinitionDTO.getName());
              metricMap.put(QUERY_SELECTOR_PARAM, metricDefinitionDTO.getPath());
              return metricMap;
            }).collect(Collectors.toList());

    dslEnvVariables.put("queries", queryData);

    return dslEnvVariables;
  }

  @Override
  public String getBaseUrl(DynatraceConnectorDTO dynatraceConnectorDTO) {
    return dynatraceConnectorDTO.getUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(DynatraceConnectorDTO dynatraceConnectorDTO) {
    return DynatraceUtils.collectionHeaders(dynatraceConnectorDTO);
  }

  @Override
  public Map<String, String> collectionParams(DynatraceConnectorDTO dynatraceConnectorDTO) {
    return Collections.emptyMap();
  }

  @Data
  @Builder
  public static class MetricCollectionInfo {
    String identifier;
    String metricName;
    String query;
  }
}
