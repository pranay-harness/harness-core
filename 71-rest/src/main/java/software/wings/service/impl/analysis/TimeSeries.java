package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 3/14/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeries {
  @NotNull private String txnName;
  @NotNull private String url;
  @NotNull private String metricName;
  @NotNull private String metricType;
}
