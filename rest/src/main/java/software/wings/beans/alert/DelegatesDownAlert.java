package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class DelegatesDownAlert implements AlertData {
  private String ip;
  private String hostName;
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    DelegatesDownAlert delegatesDownAlert = (DelegatesDownAlert) alertData;
    return StringUtils.equals(accountId, delegatesDownAlert.getAccountId())
        && StringUtils.equals(ip, delegatesDownAlert.getIp())
        && StringUtils.equals(hostName, delegatesDownAlert.getHostName());
  }

  @Override
  public String buildTitle() {
    return String.format("Delegate %s with IP %s is down", hostName, ip);
  }
}
