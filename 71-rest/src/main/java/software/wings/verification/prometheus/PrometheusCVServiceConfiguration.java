package software.wings.verification.prometheus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrometheusCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Metrics To Monitor") private List<TimeSeries> timeSeriesToAnalyze;

  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class PrometheusCVConfigurationYaml extends CVConfigurationYaml {
    private List<TimeSeries> timeSeriesList;
  }
}
