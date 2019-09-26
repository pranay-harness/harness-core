package software.wings.service.impl.apm;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.APMFetchConfig;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class APMSetupTestNodeData extends SetupTestNodeData {
  APMFetchConfig fetchConfig;
  MetricCollectionInfo apmMetricCollectionInfo;
  String host;
}
