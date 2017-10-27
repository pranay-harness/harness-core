package software.wings.beans.alert;

import com.google.inject.Injector;

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
  public boolean matches(AlertData alertData, Injector injector) {
    return approvalId.equals(((ApprovalAlert) alertData).getApprovalId());
  }

  @Override
  public String buildTitle(Injector injector) {
    return name + " needs approval";
  }
}
