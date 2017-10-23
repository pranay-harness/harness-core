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
    return approvalId.equals(((ApprovalAlert) alertData).getApprovalId());
  }

  @Override
  public String buildTitle() {
    return name + " needs approval";
  }
}
