package io.harness.cvng.core.beans.params.filterParams;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Data
@NoArgsConstructor
public class TimeSeriesAnalysisFilter extends AnalysisFilter {
  @DefaultValue("false") @QueryParam("anomalousMetricsOnly") boolean anomalous;
}
