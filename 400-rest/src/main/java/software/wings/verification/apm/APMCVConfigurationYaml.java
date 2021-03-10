package software.wings.verification.apm;

import software.wings.sm.states.APMVerificationState;
import software.wings.verification.CVConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class APMCVConfigurationYaml extends CVConfiguration.CVConfigurationYaml {
  private List<APMVerificationState.MetricCollectionInfo> metricCollectionInfos;
}
