package software.wings.service.impl.dynatrace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynaTraceMetricDataResponse {
  private DynaTraceMetricDataResult result;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DynaTraceMetricDataResult {
    private Map<String, List<List<Double>>> dataPoints;
    private String timeseriesId;
    private Map<String, String> entities;
    private String host;
  }
}
