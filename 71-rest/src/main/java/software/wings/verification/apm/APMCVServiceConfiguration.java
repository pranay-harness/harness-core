package software.wings.verification.apm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;
import software.wings.verification.CVConfiguration;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class APMCVServiceConfiguration extends CVConfiguration {
  private List<MetricCollectionInfo> metricCollectionInfos;
}
