package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class PerpetualTaskAlert implements AlertData {
  private String accountId;
  private String taskId;
  private String perpetualTaskType;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    PerpetualTaskAlert otherAlert = (PerpetualTaskAlert) alertData;
    return StringUtils.equals(accountId, otherAlert.getAccountId())
        && StringUtils.equals(taskId, otherAlert.getTaskId());
  }

  @Override
  public String buildTitle() {
    return message;
  }
}