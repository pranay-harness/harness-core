package software.wings.service.impl.apm;

import lombok.Builder;
import lombok.Data;
import software.wings.metrics.MetricType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class APMMetricInfo {
  private String url;
  private String transactionName;
  private String metricName;
  private String hostName;
  private Map<String, String> headers;
  private Map<String, String> options;
  private List<ResponseMapper> responseMappers;
  private MetricType metricType;
  private String tag;

  public APMMetricInfo() {
    this.headers = new HashMap<>();
    this.options = new HashMap<>();
    this.responseMappers = new ArrayList<>();
  }

  @Data
  @Builder
  public static class ResponseMapper {
    private String fieldName;
    private String jsonPath;
    private List<String> regexs;
  }

  public void buildFrom(APMMetricInfo metricInfo) {
    this.url = metricInfo.getUrl();
    this.transactionName = metricInfo.getTransactionName();
    this.metricName = metricInfo.getMetricName();
    this.hostName = metricInfo.getHostName();
    this.headers.putAll(metricInfo.getHeaders());
    this.options.putAll(metricInfo.getOptions());
    this.responseMappers.addAll(metricInfo.getResponseMappers());
  }
}
