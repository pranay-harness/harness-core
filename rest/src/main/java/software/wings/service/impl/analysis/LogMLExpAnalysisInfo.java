package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import software.wings.sm.StateType;

@Data
@Builder
public class LogMLExpAnalysisInfo {
  private String stateExecutionId;
  private String appId;
  private StateType stateType;
  private String expName;
  private String envId;
  private String workflowExecutionId;
  private long createdAt;
}
