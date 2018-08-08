package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.WorkflowType;

/**
 * Created by sgurubelli on 10/18/17.
 */
@Data
@Builder
public class ApprovalNeededAlert implements AlertData {
  private String executionId;
  private String approvalId;
  private String name;
  private String envId;
  private WorkflowType workflowType;
  private String workflowExecutionId;
  private String pipelineExecutionId;

  @Override
  public boolean matches(AlertData alertData) {
    return approvalId.equals(((ApprovalNeededAlert) alertData).getApprovalId());
  }

  @Override
  public String buildTitle() {
    return name + " needs approval";
  }
}
