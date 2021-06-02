package software.wings.service.impl.apm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.metrics.MetricType;
import software.wings.sm.states.APMVerificationState.Method;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class APMMetricInfo {
  private String metricName;
  private Map<String, ResponseMapper> responseMappers;
  private MetricType metricType;
  private String tag;
  private Method method;
  private String body;
  private String url;
  private String hostName;

  @Data
  @Builder
  public static class ResponseMapper implements Cloneable {
    private String fieldName;
    private String fieldValue;
    private String jsonPath;
    private List<String> regexs;
    private String timestampFormat;

    @Override
    public ResponseMapper clone() {
      return ResponseMapper.builder()
          .fieldName(fieldName)
          .fieldValue(fieldValue)
          .jsonPath(jsonPath)
          .regexs(isEmpty(regexs) ? regexs : Lists.newArrayList(regexs))
          .build();
    }
  }
}
