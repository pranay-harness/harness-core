package software.wings.graphql.datafetcher.secrets;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.NTLM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.graphql.schema.type.secrets.QLAuthScheme;
import software.wings.graphql.schema.type.secrets.QLWinRMCredential;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialInput;
import software.wings.service.intfc.SettingsService;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class WinRMCredentialController {
  @Inject SettingsService settingsService;
  public QLWinRMCredential populateWinRMCredential(@NotNull SettingAttribute settingAttribute) {
    QLAuthScheme authScheme = QLAuthScheme.NTLM;
    WinRmConnectionAttributes winRmConnectionAttributes = (WinRmConnectionAttributes) settingAttribute.getValue();
    return QLWinRMCredential.builder()
        .id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .authenticationScheme(authScheme)
        .userName(winRmConnectionAttributes.getUsername())
        .domain(winRmConnectionAttributes.getDomain())
        .useSSL(winRmConnectionAttributes.isUseSSL())
        .skipCertCheck(winRmConnectionAttributes.isSkipCertChecks())
        .port(winRmConnectionAttributes.getPort())
        .build();
  }

  private void validateSettingAttribute(QLWinRMCredentialInput winRMCredentialInput) {
    if (isBlank(winRMCredentialInput.getUserName())) {
      throw new InvalidRequestException("The username cannot be blank for the winRM credential input");
    }

    if (isBlank(winRMCredentialInput.getPassword())) {
      throw new InvalidRequestException("The password cannot be blank for the winRM credential input");
    }

    if (isBlank(winRMCredentialInput.getName())) {
      throw new InvalidRequestException("The name of the winrm credential cannot be blank");
    }
  }

  public SettingAttribute createSettingAttribute(
      @NotNull QLWinRMCredentialInput winRMCredentialInput, String accountId) {
    validateSettingAttribute(winRMCredentialInput);
    WinRmConnectionAttributes.AuthenticationScheme authenticationScheme = NTLM;
    boolean skipCertChecks = false;
    boolean useSSL = false;
    if (winRMCredentialInput.getSkipCertCheck().booleanValue()) {
      skipCertChecks = winRMCredentialInput.getSkipCertCheck().booleanValue();
    }
    if (winRMCredentialInput.getUseSSL().booleanValue()) {
      useSSL = winRMCredentialInput.getUseSSL().booleanValue();
    }
    String domain = "";
    if (winRMCredentialInput.getDomain() != null) {
      domain = winRMCredentialInput.getDomain();
    }
    WinRmConnectionAttributes settingValue = WinRmConnectionAttributes.builder()
                                                 .username(winRMCredentialInput.getUserName())
                                                 .password(winRMCredentialInput.getPassword().toCharArray())
                                                 .authenticationScheme(authenticationScheme)
                                                 .port(winRMCredentialInput.getPort())
                                                 .skipCertChecks(skipCertChecks)
                                                 .accountId(accountId)
                                                 .useSSL(useSSL)
                                                 .domain(domain)
                                                 .build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withCategory(SettingAttribute.SettingCategory.SETTING)
                                            .withValue(settingValue)
                                            .withAccountId(accountId)
                                            .withName(winRMCredentialInput.getName())
                                            .build();

    return settingsService.saveWithPruning(settingAttribute, GLOBAL_APP_ID, accountId);
  }
}
