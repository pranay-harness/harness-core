package software.wings.beans.alert;

import static java.lang.String.format;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import software.wings.utils.KubernetesConvention;

@Data
@Builder
public class DelegatesDownAlert implements AlertData {
  private String obfuscatedIpAddress;
  private String hostName;
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    DelegatesDownAlert delegatesDownAlert = (DelegatesDownAlert) alertData;
    return StringUtils.equals(accountId, delegatesDownAlert.getAccountId())
        && StringUtils.equals(hostName, delegatesDownAlert.getHostName())
        && (hostName.contains(KubernetesConvention.getAccountIdentifier(accountId))
               || StringUtils.equals(obfuscatedIpAddress, delegatesDownAlert.getObfuscatedIpAddress()));
  }

  @Override
  public String buildTitle() {
    return format("Delegate %s is down", hostName);
  }
}
