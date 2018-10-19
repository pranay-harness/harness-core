package software.wings.beans.alert;

import static java.lang.String.format;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import software.wings.utils.KubernetesConvention;

@Data
@Builder
public class DelegateProfileErrorAlert implements AlertData {
  private String accountId;
  private String ip;
  private String hostName;

  @Override
  public boolean matches(AlertData alertData) {
    DelegateProfileErrorAlert delegateProfileErrorAlert = (DelegateProfileErrorAlert) alertData;
    return StringUtils.equals(accountId, delegateProfileErrorAlert.getAccountId())
        && StringUtils.equals(hostName, delegateProfileErrorAlert.getHostName())
        && (hostName.contains(KubernetesConvention.getAccountIdentifier(accountId))
               || StringUtils.equals(ip, delegateProfileErrorAlert.getIp()));
  }

  @Override
  public String buildTitle() {
    return format("Delegate %s has a profile error", hostName);
  }
}
