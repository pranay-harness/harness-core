package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.StateType;

@Data
@Builder
public class LogMLExpAnalysisInfo {
  private String stateExecutionId;
  private String applicationId;
  private StateType stateType;
}
