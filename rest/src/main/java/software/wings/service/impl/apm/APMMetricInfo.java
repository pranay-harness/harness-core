package software.wings.service.impl.apm;

import lombok.Builder;
import lombok.Data;
import software.wings.metrics.MetricType;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMMetricInfo {
  private String metricName;
  private Map<String, ResponseMapper> responseMappers;
  private MetricType metricType;
  private String tag;

  @Data
  @Builder
  public static class ResponseMapper {
    private String fieldName;
    private String fieldValue;
    private String jsonPath;
    private List<String> regexs;
    private String timestampFormat;
  }
}
