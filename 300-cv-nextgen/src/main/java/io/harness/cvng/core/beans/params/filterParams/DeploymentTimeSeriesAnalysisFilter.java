package io.harness.cvng.core.beans.params.filterParams;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Value
@NoArgsConstructor
public class DeploymentTimeSeriesAnalysisFilter extends TimeSeriesAnalysisFilter {
  @QueryParam("hostName") String hostName;

  public boolean filterByHostName() {
    return isNotEmpty(hostName);
  }
}
