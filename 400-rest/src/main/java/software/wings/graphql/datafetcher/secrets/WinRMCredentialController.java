package software.wings.graphql.datafetcher.secrets;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.NTLM;
import static software.wings.settings.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.graphql.schema.type.secrets.QLAuthScheme;
import software.wings.graphql.schema.type.secrets.QLKerberosWinRMAuthentication;
import software.wings.graphql.schema.type.secrets.QLNtlmAuthentication;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.graphql.schema.type.secrets.QLWinRMCredential;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialInput;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialUpdate;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDP)
public class WinRMCredentialController {
  @Inject SettingsService settingService;
  @Inject SecretManager secretManager;
  @Inject UsageScopeController usageScopeController;

  public QLWinRMCredential populateWinRMCredential(@NotNull SettingAttribute settingAttribute) {
    QLAuthScheme authScheme;
    WinRmConnectionAttributes winRmConnectionAttributes = (WinRmConnectionAttributes) settingAttribute.getValue();
    switch (winRmConnectionAttributes.getAuthenticationScheme()) {
      case KERBEROS: {
        authScheme = QLAuthScheme.KERBEROS;
        break;
      }
      case NTLM: {
        authScheme = QLAuthScheme.NTLM;
        break;
      }
      default:
        throw new InvalidRequestException(
            "Unknown authentication schema " + winRmConnectionAttributes.getAuthenticationScheme(),
            WingsException.USER);
    }
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
        .usageScope(usageScopeController.populateUsageScope(settingAttribute.getUsageRestrictions()))
        .build();
  }

  private void validateSettingAttribute(QLWinRMCredentialInput winRMCredentialInput, String accountId) {
    if (isBlank(winRMCredentialInput.getUserName())) {
      throw new InvalidRequestException("The username cannot be blank for the winRM credential input");
    }

    if (winRMCredentialInput.getAuthenticationScheme().equals(QLAuthScheme.KERBEROS)) {
      if (winRMCredentialInput.getQlKerberosWinRMAuthentication() == null) {
        throw new InvalidRequestException("Kerberos Authentication can not be empty");
      }
      verifyKerberosAuth(winRMCredentialInput.getQlKerberosWinRMAuthentication(), accountId);
    } else {
      if (winRMCredentialInput.getQlNtlmAuthentication() == null) {
        throw new InvalidRequestException("NTLM Authentication can not be empty");
      }
      verifyNtlmAuth(winRMCredentialInput.getQlNtlmAuthentication(), accountId);
    }

    if (isBlank(winRMCredentialInput.getName())) {
      throw new InvalidRequestException("The name of the winRM credential cannot be blank");
    }
  }

  public SettingAttribute createSettingAttribute(
      @NotNull QLWinRMCredentialInput winRMCredentialInput, String accountId) {
    validateSettingAttribute(winRMCredentialInput, accountId);
    WinRmConnectionAttributes.AuthenticationScheme authenticationScheme;
    switch (winRMCredentialInput.getAuthenticationScheme()) {
      case KERBEROS: {
        authenticationScheme = KERBEROS;
        break;
      }
      case NTLM: {
        authenticationScheme = NTLM;
        break;
      }
      default:
        throw new InvalidRequestException(
            "Unknown authentication scheme " + winRMCredentialInput.getAuthenticationScheme(), WingsException.USER);
    }
    boolean skipCertChecks = true;
    boolean useSSL = true;
    if (winRMCredentialInput.getSkipCertCheck() != null) {
      skipCertChecks = winRMCredentialInput.getSkipCertCheck().booleanValue();
    }
    if (winRMCredentialInput.getUseSSL() != null) {
      useSSL = winRMCredentialInput.getUseSSL().booleanValue();
    }
    String domain = "";
    if (winRMCredentialInput.getDomain() != null) {
      domain = winRMCredentialInput.getDomain();
    }
    int port = 5986;
    if (winRMCredentialInput.getPort() != null) {
      port = winRMCredentialInput.getPort();
    }
    WinRmConnectionAttributes settingValue = WinRmConnectionAttributes.builder()
                                                 .username(winRMCredentialInput.getUserName())
                                                 .authenticationScheme(authenticationScheme)
                                                 .port(port)
                                                 .skipCertChecks(skipCertChecks)
                                                 .accountId(accountId)
                                                 .useSSL(useSSL)
                                                 .domain(domain)
                                                 .build();
    settingValue.setPassword(getPassword(winRMCredentialInput, accountId));
    if (winRMCredentialInput.getQlKerberosWinRMAuthentication() != null
        && winRMCredentialInput.getQlKerberosWinRMAuthentication().getKeyTabFilePath() != null) {
      settingValue.setKeyTabFilePath(winRMCredentialInput.getQlKerberosWinRMAuthentication().getKeyTabFilePath());
    }
    settingValue.setSettingType(WINRM_CONNECTION_ATTRIBUTES);
    return SettingAttribute.Builder.aSettingAttribute()
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .withValue(settingValue)
        .withAccountId(accountId)
        .withName(winRMCredentialInput.getName())
        .withUsageRestrictions(
            usageScopeController.populateUsageRestrictions(winRMCredentialInput.getUsageScope(), accountId))
        .build();
  }

  public SettingAttribute updateWinRMCredential(QLWinRMCredentialUpdate updateInput, String id, String accountId) {
    SettingAttribute existingWinRMCredential = settingService.getByAccount(accountId, id);
    if (existingWinRMCredential == null
        || existingWinRMCredential.getValue().getSettingType() != WINRM_CONNECTION_ATTRIBUTES) {
      throw new InvalidRequestException(String.format("No winRM credential exists with the id %s", id));
    }
    if (updateInput.getName().isPresent()) {
      String name = updateInput.getName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(name)) {
        throw new InvalidRequestException("Cannot set the winRM credential name as null");
      }
      existingWinRMCredential.setName(name);
    }

    WinRmConnectionAttributes settingValue = (WinRmConnectionAttributes) existingWinRMCredential.getValue();

    if (updateInput.getDomain().isPresent()) {
      String domain = updateInput.getDomain().getValue().map(StringUtils::strip).orElse(null);
      settingValue.setDomain(domain);
    }

    if (updateInput.getUserName().isPresent()) {
      String userName = updateInput.getUserName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(userName)) {
        throw new InvalidRequestException("Cannot set the username in winRM Credential as null");
      }
      settingValue.setUsername(userName);
    }

    if (updateInput.getNtlmAuthentication().isPresent()) {
      QLNtlmAuthentication ntlmAuthentication = updateInput.getNtlmAuthentication().getValue().orElse(null);
      if (ntlmAuthentication == null && updateInput.getAuthenticationScheme().equals(QLAuthScheme.NTLM)) {
        throw new InvalidRequestException("Invalid credentials Auth Scheme specified as NTLM and no NTLM input given");
      }
      if (ntlmAuthentication != null) {
        verifyNtlmAuth(ntlmAuthentication, accountId);
        if (isNotBlank(ntlmAuthentication.getPasswordSecretId())) {
          settingValue.setPassword(ntlmAuthentication.getPasswordSecretId().toCharArray());
        }
      }
    }

    if (updateInput.getKerberosWinRMAuthentication().isPresent()) {
      QLKerberosWinRMAuthentication kerberosWinRMAuthentication =
          updateInput.getKerberosWinRMAuthentication().getValue().orElse(null);
      if (kerberosWinRMAuthentication == null && updateInput.getAuthenticationScheme().equals(QLAuthScheme.KERBEROS)) {
        throw new InvalidRequestException(
            "Invalid credentials Auth Scheme specified as Kerberos and no Kerberos input given");
      }
      if (kerberosWinRMAuthentication != null) {
        verifyKerberosAuth(kerberosWinRMAuthentication, accountId);
        if (!isBlank(kerberosWinRMAuthentication.getKeyTabFilePath())) {
          settingValue.setKeyTabFilePath(kerberosWinRMAuthentication.getKeyTabFilePath());
        } else {
          settingValue.setPassword(kerberosWinRMAuthentication.getPasswordSecretId().toCharArray());
        }
      }
    }

    if (updateInput.getUseSSL().isPresent()) {
      boolean useSSL = updateInput.getUseSSL().getValue().orElse(true);
      settingValue.setUseSSL(useSSL);
    }

    if (updateInput.getSkipCertCheck().isPresent()) {
      boolean skipCertCheck = updateInput.getSkipCertCheck().getValue().orElse(true);
      settingValue.setSkipCertChecks(skipCertCheck);
    }

    if (updateInput.getPort().isPresent()) {
      Integer port = updateInput.getPort().getValue().orElse(5986);
      settingValue.setPort(port.intValue());
    }

    if (updateInput.getUsageScope().isPresent()) {
      QLUsageScope usageScope = updateInput.getUsageScope().getValue().orElse(null);
      existingWinRMCredential.setUsageRestrictions(
          usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }

    if (updateInput.getAuthenticationScheme().equals(QLAuthScheme.NTLM)
        && ((WinRmConnectionAttributes) existingWinRMCredential.getValue())
               .getAuthenticationScheme()
               .equals(KERBEROS)) {
      settingValue.setAuthenticationScheme(NTLM);
    }

    if (updateInput.getAuthenticationScheme().equals(QLAuthScheme.KERBEROS)
        && ((WinRmConnectionAttributes) existingWinRMCredential.getValue()).getAuthenticationScheme().equals(NTLM)) {
      settingValue.setAuthenticationScheme(KERBEROS);
    }

    existingWinRMCredential.setValue(settingValue);
    return settingService.updateWithSettingFields(
        existingWinRMCredential, existingWinRMCredential.getUuid(), GLOBAL_APP_ID);
  }

  void verifyKerberosAuth(QLKerberosWinRMAuthentication kerberosWinRMAuthentication, String accountId) {
    if (!isBlank(kerberosWinRMAuthentication.getKeyTabFilePath())
        && !isBlank(kerberosWinRMAuthentication.getPasswordSecretId())) {
      throw new InvalidRequestException("Cannot set both KeyTabFilePath and password secret");
    }
    if (isBlank(kerberosWinRMAuthentication.getKeyTabFilePath())) {
      if (isBlank(kerberosWinRMAuthentication.getPasswordSecretId())
          || secretManager.getSecretById(accountId, kerberosWinRMAuthentication.getPasswordSecretId()) == null) {
        throw new InvalidRequestException("The password secret id is invalid for the winRM credential input");
      }
    }
  }

  void verifyNtlmAuth(QLNtlmAuthentication ntlmAuthentication, String accountId) {
    if (isBlank(ntlmAuthentication.getPasswordSecretId())
        || secretManager.getSecretById(accountId, ntlmAuthentication.getPasswordSecretId()) == null) {
      throw new InvalidRequestException("The password secret id is invalid for the winRM credential input");
    }
  }

  char[] getPassword(QLWinRMCredentialInput winRMCredentialInput, String accountId) {
    if (winRMCredentialInput.getAuthenticationScheme().equals(QLAuthScheme.NTLM)) {
      if (winRMCredentialInput.getQlNtlmAuthentication() == null) {
        throw new InvalidRequestException("NTLM Authentication can not be empty");
      }
      verifyNtlmAuth(winRMCredentialInput.getQlNtlmAuthentication(), accountId);
      return winRMCredentialInput.getQlNtlmAuthentication().getPasswordSecretId().toCharArray();
    } else {
      if (winRMCredentialInput.getQlKerberosWinRMAuthentication() == null) {
        throw new InvalidRequestException("Kerberos Authentication can not be empty");
      }
      verifyKerberosAuth(winRMCredentialInput.getQlKerberosWinRMAuthentication(), accountId);
      return ((isBlank(winRMCredentialInput.getQlKerberosWinRMAuthentication().getPasswordSecretId()))
              ? null
              : winRMCredentialInput.getQlKerberosWinRMAuthentication().getPasswordSecretId().toCharArray());
    }
  }
}
