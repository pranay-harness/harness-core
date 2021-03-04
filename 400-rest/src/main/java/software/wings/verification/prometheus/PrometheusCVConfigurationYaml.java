package software.wings.verification.prometheus;

import software.wings.service.impl.analysis.TimeSeries;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PrometheusCVConfigurationYaml extends CVConfigurationYaml {
  private List<TimeSeries> timeSeriesList;
}
