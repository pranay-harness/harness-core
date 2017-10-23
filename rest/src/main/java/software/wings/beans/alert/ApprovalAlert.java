package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 10/18/17.
 */
@Data
@Builder
public class ApprovalAlert implements AlertData {
  private String executionId;
  private String approvalId;
  private String name;

  @Override
  public boolean matches(AlertData alertData) {
    ApprovalAlert approvalAlert = (ApprovalAlert) alertData;
    return approvalId == null ? executionId.equals(approvalAlert.getExecutionId())
                              : approvalId.equals(approvalAlert.getApprovalId());
  }

  @Override
  public String buildTitle() {
    return name + " needs approval";
  }
}
