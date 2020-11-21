package software.wings.service.impl.analysis;

import software.wings.sm.StateType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpAnalysisInfo {
  private String stateExecutionId;
  private String appId;
  private StateType stateType;
  private String expName;
  private String envId;
  private String workflowExecutionId;
  private long createdAt;
  private boolean mismatch;
}
