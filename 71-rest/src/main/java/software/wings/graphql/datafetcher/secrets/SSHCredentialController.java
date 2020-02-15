package software.wings.graphql.datafetcher.secrets;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.SSH_KEY;
import static software.wings.graphql.schema.type.secrets.QLTGTGenerationUsing.KEY_TAB_FILE;
import static software.wings.graphql.schema.type.secrets.QLTGTGenerationUsing.PASSWORD;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KerberosConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.type.secrets.QLInlineSSHKey;
import software.wings.graphql.schema.type.secrets.QLKerberosAuthentication;
import software.wings.graphql.schema.type.secrets.QLKerberosAuthenticationInput;
import software.wings.graphql.schema.type.secrets.QLKerberosPassword;
import software.wings.graphql.schema.type.secrets.QLKeyTabFile;
import software.wings.graphql.schema.type.secrets.QLSSHAuthentication;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationInput;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationMethod;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationScheme;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationType;
import software.wings.graphql.schema.type.secrets.QLSSHCredential;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialInput;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialType;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialUpdate;
import software.wings.graphql.schema.type.secrets.QLSSHKeyFile;
import software.wings.graphql.schema.type.secrets.QLSSHPassword;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLTGTGenerationMethod;
import software.wings.graphql.schema.type.secrets.QLTGTGenerationUsing;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class SSHCredentialController {
  @Inject SettingsService settingsService;
  public QLSSHCredential populateSSHCredential(@NotNull SettingAttribute settingAttribute) {
    QLSSHAuthenticationType sshAuthenticationType = null;
    HostConnectionAttributes sshCreds = (HostConnectionAttributes) settingAttribute.getValue();
    if (sshCreds.getAccessType() == HostConnectionAttributes.AccessType.KERBEROS) {
      sshAuthenticationType = QLKerberosAuthentication.builder()
                                  .port(sshCreds.getSshPort())
                                  .principal(sshCreds.getKerberosConfig().getPrincipal())
                                  .realm(sshCreds.getKerberosConfig().getRealm())
                                  .build();
    } else if (sshCreds.getAccessType() == HostConnectionAttributes.AccessType.KEY
        || sshCreds.getAccessType() == HostConnectionAttributes.AccessType.USER_PASSWORD) {
      sshAuthenticationType =
          QLSSHAuthentication.builder().port(sshCreds.getSshPort()).userName(sshCreds.getUserName()).build();
    }
    return QLSSHCredential.builder()
        .id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .secretType(QLSecretType.SSH_CREDENTIAL)
        .authenticationType(sshAuthenticationType)
        .build();
  }

  @Data
  @Builder
  private static class KerberosSettings {
    int port;
    KerberosConfig kerberosConfig;
    String kerberosPassword;
    HostConnectionAttributes.AccessType accessType;
  }

  @Data
  @Builder
  private static class SSHSetting {
    String userName;
    HostConnectionAttributes.AccessType accessType;
    int port;
    String key;
    String passphrase;
    boolean keyless;
    String keyPath;
    String sshPassword;
  }

  private SSHSetting sshSettingWithInlineKey(QLInlineSSHKey inlineSSHKey, String userName, int port) {
    String passphrase = null;
    if (inlineSSHKey == null || isBlank(inlineSSHKey.getSshKey())) {
      throw new InvalidRequestException("No inline SSH key provided for the SSH credential type SSH_KEY");
    }
    String key = inlineSSHKey.getSshKey();
    passphrase = inlineSSHKey.getPassphrase();
    return SSHSetting.builder()
        .accessType(KEY)
        .port(port)
        .key(key)
        .keyless(false)
        .passphrase(passphrase)
        .userName(userName)
        .build();
  }

  private SSHSetting sshSettingWithFilePath(QLSSHKeyFile sshKeyFile, String userName, int port) {
    if (sshKeyFile == null || isBlank(sshKeyFile.getPath())) {
      throw new InvalidRequestException("No SSH key file path provided for the SSH credential type SSH_KEY_FILE_PATH");
    }
    String keyPath = sshKeyFile.getPath();
    String passphrase = sshKeyFile.getPassphrase();
    return SSHSetting.builder()
        .accessType(KEY)
        .port(port)
        .keyPath(keyPath)
        .keyless(true)
        .passphrase(passphrase)
        .userName(userName)
        .build();
  }

  private SSHSetting sshSettingWithPassword(QLSSHPassword sshPasswordInput, String userName, int port) {
    if (sshPasswordInput == null || isBlank(sshPasswordInput.getPassword())) {
      throw new InvalidRequestException("No SSH password provided for the SSH credential type PASSWORD");
    }
    String sshPassword = sshPasswordInput.getPassword();
    return SSHSetting.builder()
        .accessType(USER_PASSWORD)
        .port(port)
        .sshPassword(sshPassword)
        .userName(userName)
        .build();
  }

  private SSHSetting getSSHSettings(QLSSHAuthenticationInput sshCredential) {
    if (sshCredential == null) {
      throw new InvalidRequestException(
          "No ssh authentication input provided for the SSH credential with auth scheme SSH");
    }
    String userName = sshCredential.getUserName();
    int port = sshCredential.getPort();
    QLSSHAuthenticationMethod sshauthenticationMethod = sshCredential.getSshAuthenticationMethod();
    QLSSHCredentialType credType = sshauthenticationMethod.getSshCredentialType();
    if (credType == QLSSHCredentialType.SSH_KEY) {
      return sshSettingWithInlineKey(sshauthenticationMethod.getInlineSSHKey(), userName, port);
    } else if (credType == QLSSHCredentialType.SSH_KEY_FILE_PATH) {
      return sshSettingWithFilePath(sshauthenticationMethod.getSshKeyFile(), userName, port);
    } else {
      return sshSettingWithPassword(sshauthenticationMethod.getServerPassword(), userName, port);
    }
  }

  private KerberosSettings getKerberosSettingWithKeyTabFile(
      QLTGTGenerationMethod tgtGenerationMethod, String principal, String realm, int port) {
    QLKeyTabFile keyTabFileInput = tgtGenerationMethod.getKeyTabFile();
    if (keyTabFileInput == null || isBlank(keyTabFileInput.getFilePath())) {
      throw new InvalidRequestException(
          "No key file path provided for the kerberos credential with TGT Generation using key file path");
    }
    String keyTabFilePath = keyTabFileInput.getFilePath();
    KerberosConfig kerberosConfig = KerberosConfig.builder()
                                        .generateTGT(true)
                                        .keyTabFilePath(keyTabFilePath)
                                        .principal(principal)
                                        .realm(realm)
                                        .build();
    return KerberosSettings.builder()
        .port(port)
        .kerberosConfig(kerberosConfig)
        .accessType(HostConnectionAttributes.AccessType.KERBEROS)
        .build();
  }

  private KerberosSettings getKerberosSettingWithPassword(
      QLTGTGenerationMethod tgtGenerationMethod, String principal, String realm, int port) {
    QLKerberosPassword kerberosPasswordInput = tgtGenerationMethod.getKerberosPassword();
    if (kerberosPasswordInput == null || isBlank(kerberosPasswordInput.getPassword())) {
      throw new InvalidRequestException("No password provided for TGT generation using password");
    }
    String kerberosPassword = kerberosPasswordInput.getPassword();
    KerberosConfig kerberosConfig =
        KerberosConfig.builder().generateTGT(true).principal(principal).realm(realm).build();
    return KerberosSettings.builder()
        .port(port)
        .kerberosConfig(kerberosConfig)
        .kerberosPassword(kerberosPassword)
        .accessType(HostConnectionAttributes.AccessType.KERBEROS)
        .build();
  }

  private void validateKerberosSSHInput(QLKerberosAuthenticationInput kerberosAuthentication) {
    if (kerberosAuthentication == null) {
      throw new InvalidRequestException(
          "No kerberos authentication provided for the SSH credential with auth scheme KERBEROS");
    }
    if (isBlank(kerberosAuthentication.getPrincipal())) {
      throw new InvalidRequestException(
          " The principal cannot be blank for a ssh connection with KERBEROS auth scheme");
    }
    if (isBlank(kerberosAuthentication.getRealm())) {
      throw new InvalidRequestException(" The realm cannot be blank for a ssh connection with KERBEROS auth scheme");
    }
  }

  private KerberosSettings getKerberosSettings(QLKerberosAuthenticationInput kerberosAuthentication) {
    validateKerberosSSHInput(kerberosAuthentication);
    // Extracting port, principal, realm
    int port = kerberosAuthentication.getPort();
    String principal = kerberosAuthentication.getPrincipal();
    String realm = kerberosAuthentication.getRealm();

    QLTGTGenerationMethod tgtGenerationMethod = kerberosAuthentication.getTgtGenerationMethod();
    if (tgtGenerationMethod != null) {
      QLTGTGenerationUsing kerberosAuthUsing = tgtGenerationMethod.getTgtGenerationUsing();
      if (kerberosAuthUsing == KEY_TAB_FILE) {
        return getKerberosSettingWithKeyTabFile(tgtGenerationMethod, principal, realm, port);
      } else if (kerberosAuthUsing == PASSWORD) {
        return getKerberosSettingWithPassword(tgtGenerationMethod, principal, realm, port);
      }
    }
    KerberosConfig kerberosConfig =
        KerberosConfig.builder().generateTGT(false).principal(principal).realm(realm).build();
    return KerberosSettings.builder()
        .port(port)
        .kerberosConfig(kerberosConfig)
        .accessType(HostConnectionAttributes.AccessType.KERBEROS)
        .build();
  }

  public HostConnectionAttributes createHostConnectionAttribute(QLSSHAuthenticationScheme authSchemeInput,
      QLSSHAuthenticationInput sshSettingInput, QLKerberosAuthenticationInput kerberosSettingInput) {
    HostConnectionAttributes.AuthenticationScheme authScheme = SSH_KEY;
    HostConnectionAttributes.AccessType accessType = KEY;
    String userName = null;
    int port = 22;
    String key = null;
    String passphrase = null;
    boolean keyless = false;
    String keyPath = null;
    String sshPassword = null;
    KerberosConfig kerberosConfig = null;
    String kerberosPassword = null;
    if (authSchemeInput == QLSSHAuthenticationScheme.SSH) {
      SSHSetting sshSetting = getSSHSettings(sshSettingInput);
      accessType = sshSetting.getAccessType();
      port = sshSetting.getPort();
      key = sshSetting.getKey();
      passphrase = sshSetting.getPassphrase();
      keyless = sshSetting.isKeyless();
      keyPath = sshSetting.getKeyPath();
      userName = sshSetting.getUserName();
      sshPassword = sshSetting.getSshPassword();
    } else if (authSchemeInput == QLSSHAuthenticationScheme.KERBEROS) {
      authScheme = KERBEROS;
      KerberosSettings kerberoSettings = getKerberosSettings(kerberosSettingInput);
      accessType = kerberoSettings.getAccessType();
      kerberosConfig = kerberoSettings.getKerberosConfig();
      port = kerberoSettings.getPort();
      kerberosPassword = kerberoSettings.getKerberosPassword();
    }

    HostConnectionAttributes settingValue =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(accessType)
            .withUserName(userName)
            .withSshPort(port)
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey(key == null ? null : key.toCharArray())
            .withSshPassword(sshPassword == null ? null : sshPassword.toCharArray())
            .withKerberosPassword(kerberosPassword == null ? null : kerberosPassword.toCharArray())
            .withKeyPath(keyPath)
            .withKerberosConfig(kerberosConfig)
            .withKeyless(keyless)
            .withPassphrase(passphrase == null ? null : passphrase.toCharArray())
            .withAuthenticationScheme(authScheme)
            .build();
    settingValue.setSettingType(SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES);
    return settingValue;
  }

  public SettingAttribute createSettingAttribute(QLSSHCredentialInput sshCredentialInput, String accountId) {
    HostConnectionAttributes settingValue = createHostConnectionAttribute(sshCredentialInput.getAuthenticationScheme(),
        sshCredentialInput.getSshAuthentication(), sshCredentialInput.getKerberosAuthentication());
    return SettingAttribute.Builder.aSettingAttribute()
        .withName(sshCredentialInput.getName())
        .withValue(settingValue)
        .withAccountId(accountId)
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .build();
  }

  public SettingAttribute updateSSHCredential(QLSSHCredentialUpdate updateInput, String sshCredId, String accountId) {
    SettingAttribute existingSettingAttribute = settingsService.get(sshCredId);
    if (existingSettingAttribute == null) {
      throw new InvalidRequestException(String.format("No ssh credential exists with the id %s", sshCredId));
    }
    if (updateInput.getName().hasBeenSet()) {
      String name = updateInput.getName().getValue().map(StringUtils::strip).orElse(null);
      if (name == null) {
        throw new InvalidRequestException("Cannot set the ssh credential name as null");
      }
      existingSettingAttribute.setName(name);
    }
    boolean needToUpdateCred = false;
    QLSSHAuthenticationInput sshCredInput = null;
    QLKerberosAuthenticationInput kerberosInput = null;
    if (updateInput.getSshAuthentication().hasBeenSet()) {
      sshCredInput = updateInput.getSshAuthentication().getValue().orElse(null);
      needToUpdateCred = true;
    }
    if (updateInput.getKerberosAuthentication().hasBeenSet()) {
      kerberosInput = updateInput.getKerberosAuthentication().getValue().orElse(null);
      needToUpdateCred = true;
    }
    if (needToUpdateCred && (updateInput.getAuthenticationScheme() == null)) {
      throw new InvalidRequestException("The SSH authentication scheme is not provided with the update request");
    }
    if (needToUpdateCred) {
      HostConnectionAttributes sshSettings =
          createHostConnectionAttribute(updateInput.getAuthenticationScheme(), sshCredInput, kerberosInput);
      existingSettingAttribute.setValue(sshSettings);
    }
    return settingsService.updateWithSettingFields(
        existingSettingAttribute, existingSettingAttribute.getUuid(), GLOBAL_APP_ID);
  }
}
