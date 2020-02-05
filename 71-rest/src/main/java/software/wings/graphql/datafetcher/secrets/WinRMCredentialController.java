package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.graphql.schema.type.secrets.QLAuthScheme;
import software.wings.graphql.schema.type.secrets.QLWinRMCredential;

@Slf4j
@Singleton
public class WinRMCredentialController {
  public QLWinRMCredential populateWinRMCredential(SettingAttribute settingAttribute) {
    QLAuthScheme authScheme = QLAuthScheme.NTLM;
    WinRmConnectionAttributes winRmConnectionAttributes = (WinRmConnectionAttributes) settingAttribute.getValue();
    if (winRmConnectionAttributes.getAuthenticationScheme() == WinRmConnectionAttributes.AuthenticationScheme.NTLM) {
      authScheme = QLAuthScheme.NTLM;
    }
    return QLWinRMCredential.builder()
        .id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .authenticationScheme(authScheme)
        .username(winRmConnectionAttributes.getDomain())
        .domain(winRmConnectionAttributes.getDomain())
        .useSSL(winRmConnectionAttributes.isUseSSL())
        .skipCertCheck(winRmConnectionAttributes.isSkipCertChecks())
        .port(winRmConnectionAttributes.getPort())
        .build();
  }
}
