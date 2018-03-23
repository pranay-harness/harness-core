package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class DelegatesDownAlert implements AlertData {
  private String delegateId;
  private String accountId;
  private String hostName;

  @Override
  public boolean matches(AlertData alertData) {
    return StringUtils.equals(accountId, ((DelegatesDownAlert) alertData).getAccountId())
        && StringUtils.equals(delegateId, ((DelegatesDownAlert) alertData).getDelegateId());
  }

  @Override
  public String buildTitle() {
    return new StringBuilder()
        .append("Delegate down for accountId: ")
        .append(accountId)
        .append(", with hostname: ")
        .append(hostName)
        .toString();
  }
}
