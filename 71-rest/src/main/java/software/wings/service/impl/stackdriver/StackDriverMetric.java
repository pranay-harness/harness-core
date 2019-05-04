package software.wings.service.impl.stackdriver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 11/27/2018
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class StackDriverMetric {
  private String metricName;
  private String metric;
  private String displayName;
  private String unit;
  private String kind;
  private String valueType;
}
