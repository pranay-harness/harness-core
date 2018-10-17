package software.wings.verification.prometheus;

import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.verification.CVConfiguration;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Metrics To Monitor") private List<TimeSeries> timeSeriesToAnalyze;
}
