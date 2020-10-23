package io.harness.delegate.beans.alert;

import static java.lang.String.format;

import io.harness.alert.AlertData;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class DelegatesScalingGroupDownAlert implements AlertData {
  private String groupName;
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    DelegatesScalingGroupDownAlert delegatesDownAlert = (DelegatesScalingGroupDownAlert) alertData;
    return StringUtils.equals(accountId, delegatesDownAlert.getAccountId())
        && StringUtils.equals(groupName, delegatesDownAlert.getGroupName());
  }

  @Override
  public String buildTitle() {
    return format("Delegate group %s is down", groupName);
  }
}
