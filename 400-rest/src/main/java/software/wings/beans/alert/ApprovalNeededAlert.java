package software.wings.beans.alert;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.WorkflowType;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 10/18/17.
 */
@Data
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
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
