package software.wings.beans.alert;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@TargetModule(HarnessModule._955_ALERT_BEANS)
public class PerpetualTaskAlert implements AlertData {
  private String accountId;
  private String description;
  private String perpetualTaskType;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    PerpetualTaskAlert otherAlert = (PerpetualTaskAlert) alertData;
    return StringUtils.equals(accountId, otherAlert.getAccountId())
        && StringUtils.equals(perpetualTaskType, otherAlert.getPerpetualTaskType());
  }

  @Override
  public String buildTitle() {
    if (StringUtils.isNotBlank(description)) {
      return message + String.format(" For example %s.", description);
    }

    return message;
  }
}
