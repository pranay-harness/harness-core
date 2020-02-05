package software.wings.graphql.datafetcher.secrets;

import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.NTLM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextInput;
import software.wings.graphql.schema.type.secrets.QLWinRMCredentialInput;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
@Singleton
public class SecretController {
  @Inject SecretManager secretManager;

  public String createEncryptedText(QLCreateSecretInput input, String accountId) {
    String secretId = null;
    QLEncryptedTextInput encryptedText = input.getEncryptedText();
    if (encryptedText == null) {
      throw new InvalidRequestException("No encrypted text input provided in the request");
    }
    return secretManager.saveSecret(
        accountId, encryptedText.getSecretManagerId(), encryptedText.getName(), encryptedText.getValue(), null, null);
  }

  public SettingAttribute createSettingAttribute(QLWinRMCredentialInput winRMCredentialInput) {
    AuthenticationScheme authenticationScheme = NTLM;
    boolean skipCertChecks = false;
    boolean useSSL = false;
    if (winRMCredentialInput.isSkipCertCheck() == true) {
      skipCertChecks = true;
    }
    if (winRMCredentialInput.isUseSSL() == true) {
      useSSL = true;
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
                                                 .useSSL(useSSL)
                                                 .domain(domain)
                                                 .build();
    return SettingAttribute.Builder.aSettingAttribute()
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .withValue(settingValue)
        .withName(winRMCredentialInput.getName())
        .build();
  }
}
