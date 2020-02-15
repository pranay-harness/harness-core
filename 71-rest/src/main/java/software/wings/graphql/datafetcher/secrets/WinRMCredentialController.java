package software.wings.graphql.datafetcher.secrets;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.NTLM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.graphql.schema.type.secrets.QLAuthScheme;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLWinRMCredential;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialInput;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialUpdate;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class WinRMCredentialController {
  @Inject SettingsService settingService;
  public QLWinRMCredential populateWinRMCredential(@NotNull SettingAttribute settingAttribute) {
    QLAuthScheme authScheme = QLAuthScheme.NTLM;
    WinRmConnectionAttributes winRmConnectionAttributes = (WinRmConnectionAttributes) settingAttribute.getValue();
    return QLWinRMCredential.builder()
        .id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .authenticationScheme(authScheme)
        .secretType(QLSecretType.WINRM_CREDENTIAL)
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
    settingValue.setSettingType(SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES);
    return SettingAttribute.Builder.aSettingAttribute()
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .withValue(settingValue)
        .withAccountId(accountId)
        .withName(winRMCredentialInput.getName())
        .build();
  }

  public SettingAttribute updateWinRMCredential(QLWinRMCredentialUpdate updateInput, String id, String accountId) {
    SettingAttribute existingWinRMCredential = settingService.get(id);
    if (existingWinRMCredential == null) {
      throw new InvalidRequestException(String.format("No winRM credential exists with the id %s", id));
    }
    if (updateInput.getName().hasBeenSet()) {
      String name = updateInput.getName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(name)) {
        throw new InvalidRequestException("Cannot set the winRM credential name as null");
      }
      existingWinRMCredential.setName(name);
    }

    WinRmConnectionAttributes settingValue = (WinRmConnectionAttributes) existingWinRMCredential.getValue();

    if (updateInput.getDomain().hasBeenSet()) {
      String domain = updateInput.getDomain().getValue().map(StringUtils::strip).orElse(null);
      settingValue.setDomain(domain);
    }

    if (updateInput.getUserName().hasBeenSet()) {
      String userName = updateInput.getUserName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(userName)) {
        throw new InvalidRequestException("Cannot set the username in wirRM Credential as null");
      }
      settingValue.setUsername(userName);
    }

    if (updateInput.getPassword().hasBeenSet()) {
      String password = updateInput.getPassword().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(password)) {
        throw new InvalidRequestException("Cannot set the password in wirRM Credential as null");
      }
      settingValue.setPassword(password.toCharArray());
    }

    if (updateInput.getUseSSL().hasBeenSet()) {
      boolean useSSL = updateInput.getUseSSL().getValue().orElse(false);
      settingValue.setUseSSL(useSSL);
    }

    if (updateInput.getSkipCertCheck().hasBeenSet()) {
      boolean skipCertCheck = updateInput.getSkipCertCheck().getValue().orElse(false);
      settingValue.setSkipCertChecks(skipCertCheck);
    }

    if (updateInput.getPort().hasBeenSet()) {
      Integer port = updateInput.getPort().getValue().orElse(22);
      settingValue.setPort(port.intValue());
    }

    existingWinRMCredential.setValue(settingValue);
    return settingService.updateWithSettingFields(
        existingWinRMCredential, existingWinRMCredential.getUuid(), GLOBAL_APP_ID);
  }
}
