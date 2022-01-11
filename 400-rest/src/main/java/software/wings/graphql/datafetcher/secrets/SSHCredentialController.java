/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secrets;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.shell.AccessType.KEY;
import static io.harness.shell.AccessType.USER_PASSWORD;
import static io.harness.shell.AuthenticationScheme.KERBEROS;
import static io.harness.shell.AuthenticationScheme.SSH_KEY;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.graphql.schema.type.secrets.QLTGTGenerationUsing.KEY_TAB_FILE;
import static software.wings.graphql.schema.type.secrets.QLTGTGenerationUsing.PASSWORD;
import static software.wings.settings.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.shell.AccessType;
import io.harness.shell.AuthenticationScheme;
import io.harness.shell.KerberosConfig;

import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.type.secrets.QLInlineSSHKey;
import software.wings.graphql.schema.type.secrets.QLInlineSSHKeyOutput;
import software.wings.graphql.schema.type.secrets.QLKerberosAuthentication;
import software.wings.graphql.schema.type.secrets.QLKerberosAuthenticationInput;
import software.wings.graphql.schema.type.secrets.QLKerberosPassword;
import software.wings.graphql.schema.type.secrets.QLKeyTabFile;
import software.wings.graphql.schema.type.secrets.QLSSHAuthentication;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationInput;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationMethod;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationMethodOutput;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationScheme;
import software.wings.graphql.schema.type.secrets.QLSSHAuthenticationType;
import software.wings.graphql.schema.type.secrets.QLSSHCredential;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialInput;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialType;
import software.wings.graphql.schema.type.secrets.QLSSHCredentialUpdate;
import software.wings.graphql.schema.type.secrets.QLSSHKeyFile;
import software.wings.graphql.schema.type.secrets.QLSSHKeyFileOutput;
import software.wings.graphql.schema.type.secrets.QLSSHPassword;
import software.wings.graphql.schema.type.secrets.QLSSHPasswordOutput;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLTGTGenerationMethod;
import software.wings.graphql.schema.type.secrets.QLTGTGenerationUsing;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDP)
public class SSHCredentialController {
  @Inject SettingsService settingsService;
  @Inject SecretManager secretManager;
  @Inject UsageScopeController usageScopeController;

  public QLSSHCredential populateSSHCredential(@NotNull SettingAttribute settingAttribute) {
    QLSSHAuthenticationType sshAuthenticationType = null;
    HostConnectionAttributes sshCreds = (HostConnectionAttributes) settingAttribute.getValue();
    if (sshCreds.getAccessType() == AccessType.KERBEROS) {
      sshAuthenticationType = QLKerberosAuthentication.builder()
                                  .port(sshCreds.getSshPort())
                                  .principal(sshCreds.getKerberosConfig().getPrincipal())
                                  .realm(sshCreds.getKerberosConfig().getRealm())
                                  .build();
    } else if (sshCreds.getAccessType() == AccessType.KEY) {
      if (!sshCreds.isKeyless()) {
        QLInlineSSHKeyOutput qlInlineSSHKeyOutput = QLInlineSSHKeyOutput.builder()
                                                        .sshKeySecretFileId(sshCreds.getEncryptedKey())
                                                        .passphraseSecretId(sshCreds.getEncryptedPassphrase())
                                                        .build();
        QLSSHAuthenticationMethodOutput qlsshAuthenticationMethodOutput = QLSSHAuthenticationMethodOutput.builder()
                                                                              .sshCredentialType("SSH_KEY")
                                                                              .inlineSSHKey(qlInlineSSHKeyOutput)
                                                                              .build();
        sshAuthenticationType = QLSSHAuthentication.builder()
                                    .port(sshCreds.getSshPort())
                                    .userName(sshCreds.getUserName())
                                    .sshAuthenticationMethod(qlsshAuthenticationMethodOutput)
                                    .build();
      } else {
        QLSSHKeyFileOutput qlsshKeyFileOutput = QLSSHKeyFileOutput.builder()
                                                    .passphraseSecretId(sshCreds.getEncryptedPassphrase())
                                                    .path(sshCreds.getKeyPath())
                                                    .build();
        QLSSHAuthenticationMethodOutput qlsshAuthenticationMethodOutput = QLSSHAuthenticationMethodOutput.builder()
                                                                              .sshCredentialType("SSH_KEY_FILE_PATH")
                                                                              .sshKeyFile(qlsshKeyFileOutput)
                                                                              .build();
        sshAuthenticationType = QLSSHAuthentication.builder()
                                    .port(sshCreds.getSshPort())
                                    .userName(sshCreds.getUserName())
                                    .sshAuthenticationMethod(qlsshAuthenticationMethodOutput)
                                    .build();
      }
    } else if (sshCreds.getAccessType() == AccessType.USER_PASSWORD) {
      QLSSHPasswordOutput qlsshPasswordOutput =
          QLSSHPasswordOutput.builder().passwordSecretId(sshCreds.getEncryptedSshPassword()).build();
      QLSSHAuthenticationMethodOutput qlsshAuthenticationMethodOutput = QLSSHAuthenticationMethodOutput.builder()
                                                                            .sshCredentialType("PASSWORD")
                                                                            .serverPassword(qlsshPasswordOutput)
                                                                            .build();
      sshAuthenticationType = QLSSHAuthentication.builder()
                                  .port(sshCreds.getSshPort())
                                  .userName(sshCreds.getUserName())
                                  .sshAuthenticationMethod(qlsshAuthenticationMethodOutput)
                                  .build();
    }
    return QLSSHCredential.builder()
        .id(settingAttribute.getUuid())
        .name(settingAttribute.getName())
        .secretType(QLSecretType.SSH_CREDENTIAL)
        .authenticationType(sshAuthenticationType)
        .usageScope(usageScopeController.populateUsageScope(settingAttribute.getUsageRestrictions()))
        .build();
  }

  @Data
  @Builder
  private static class KerberosSettings {
    int port;
    KerberosConfig kerberosConfig;
    String kerberosPasswordSecretId;
    AccessType accessType;
  }

  @Data
  @Builder
  private static class SSHSetting {
    String userName;
    AccessType accessType;
    int port;
    String keySecretFileId;
    String passphraseSecretId;
    boolean keyless;
    String keyPath;
    String sshPasswordSecretId;
  }

  private SSHSetting sshSettingWithInlineKey(QLInlineSSHKey inlineSSHKey, String userName, int port) {
    if (inlineSSHKey == null || isBlank(inlineSSHKey.getSshKeySecretFileId())) {
      throw new InvalidRequestException("No SSH key secret file id provided for the SSH credential type SSH_KEY");
    }
    return SSHSetting.builder()
        .accessType(KEY)
        .port(port)
        .keySecretFileId(inlineSSHKey.getSshKeySecretFileId())
        .keyless(false)
        .passphraseSecretId(inlineSSHKey.getPassphraseSecretId())
        .userName(userName)
        .build();
  }

  private SSHSetting sshSettingWithFilePath(QLSSHKeyFile sshKeyFile, String userName, int port) {
    if (sshKeyFile == null || isBlank(sshKeyFile.getPath())) {
      throw new InvalidRequestException("No SSH key file path provided for the SSH credential type SSH_KEY_FILE_PATH");
    }
    String keyPath = sshKeyFile.getPath();
    return SSHSetting.builder()
        .accessType(KEY)
        .port(port)
        .keyPath(keyPath)
        .keyless(true)
        .passphraseSecretId(sshKeyFile.getPassphraseSecretId())
        .userName(userName)
        .build();
  }

  private SSHSetting sshSettingWithPassword(QLSSHPassword sshPasswordInput, String userName, int port) {
    if (sshPasswordInput == null || isBlank(sshPasswordInput.getPasswordSecretId())) {
      throw new InvalidRequestException("No SSH password secret id provided for the SSH credential type PASSWORD");
    }
    return SSHSetting.builder()
        .accessType(USER_PASSWORD)
        .port(port)
        .sshPasswordSecretId(sshPasswordInput.getPasswordSecretId())
        .userName(userName)
        .build();
  }

  private SSHSetting getSSHSettings(QLSSHAuthenticationInput sshCredential) {
    if (sshCredential == null) {
      throw new InvalidRequestException(
          "No ssh authentication input provided for the SSH credential with auth scheme SSH");
    }
    String userName = sshCredential.getUserName();
    if (isBlank(userName)) {
      throw new InvalidRequestException("The user name provided with the request cannot be blank");
    }
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
    return KerberosSettings.builder().port(port).kerberosConfig(kerberosConfig).accessType(AccessType.KERBEROS).build();
  }

  private KerberosSettings getKerberosSettingWithPassword(
      QLTGTGenerationMethod tgtGenerationMethod, String principal, String realm, int port) {
    QLKerberosPassword kerberosPasswordInput = tgtGenerationMethod.getKerberosPassword();
    if (kerberosPasswordInput == null || isBlank(kerberosPasswordInput.getPasswordSecretId())) {
      throw new InvalidRequestException("No password secret id provided for TGT generation using password");
    }
    KerberosConfig kerberosConfig =
        KerberosConfig.builder().generateTGT(true).principal(principal).realm(realm).build();
    return KerberosSettings.builder()
        .port(port)
        .kerberosConfig(kerberosConfig)
        .kerberosPasswordSecretId(kerberosPasswordInput.getPasswordSecretId())
        .accessType(AccessType.KERBEROS)
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
    return KerberosSettings.builder().port(port).kerberosConfig(kerberosConfig).accessType(AccessType.KERBEROS).build();
  }

  private HostConnectionAttributes createHostConnectionAttribute(String accountId,
      QLSSHAuthenticationScheme authSchemeInput, QLSSHAuthenticationInput sshSettingInput,
      QLKerberosAuthenticationInput kerberosSettingInput) {
    AuthenticationScheme authScheme = SSH_KEY;
    AccessType accessType = KEY;
    String userName = null;
    int port = 22;
    String keySecretFileId = null;
    String passphraseSecretId = null;
    boolean keyless = false;
    String keyPath = null;
    String sshPasswordSecretId = null;
    KerberosConfig kerberosConfig = null;
    String kerberosPasswordSecretId = null;
    if (authSchemeInput == QLSSHAuthenticationScheme.SSH) {
      SSHSetting sshSetting = getSSHSettings(sshSettingInput);
      accessType = sshSetting.getAccessType();
      port = sshSetting.getPort();
      keySecretFileId = sshSetting.getKeySecretFileId();
      validateSecret(accountId, keySecretFileId);
      passphraseSecretId = sshSetting.getPassphraseSecretId();
      validateSecret(accountId, passphraseSecretId);
      keyless = sshSetting.isKeyless();
      keyPath = sshSetting.getKeyPath();
      userName = sshSetting.getUserName();
      sshPasswordSecretId = sshSetting.getSshPasswordSecretId();
      validateSecret(accountId, sshPasswordSecretId);
    } else if (authSchemeInput == QLSSHAuthenticationScheme.KERBEROS) {
      authScheme = KERBEROS;
      KerberosSettings kerberoSettings = getKerberosSettings(kerberosSettingInput);
      accessType = kerberoSettings.getAccessType();
      kerberosConfig = kerberoSettings.getKerberosConfig();
      port = kerberoSettings.getPort();
      kerberosPasswordSecretId = kerberoSettings.getKerberosPasswordSecretId();
      validateSecret(accountId, kerberosPasswordSecretId);
    }

    HostConnectionAttributes settingValue =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(accessType)
            .withUserName(userName)
            .withSshPort(port)
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey(keySecretFileId == null ? null : keySecretFileId.toCharArray())
            .withSshPassword(sshPasswordSecretId == null ? null : sshPasswordSecretId.toCharArray())
            .withKerberosPassword(kerberosPasswordSecretId == null ? null : kerberosPasswordSecretId.toCharArray())
            .withKeyPath(keyPath)
            .withKerberosConfig(kerberosConfig)
            .withKeyless(keyless)
            .withPassphrase(passphraseSecretId == null ? null : passphraseSecretId.toCharArray())
            .withAuthenticationScheme(authScheme)
            .build();
    settingValue.setSettingType(HOST_CONNECTION_ATTRIBUTES);
    return settingValue;
  }

  private void validateSecret(String accountId, String secretId) {
    if (isNotBlank(secretId) && secretManager.getSecretById(accountId, secretId) == null) {
      throw new InvalidRequestException("Invalid input secret");
    }
  }

  public SettingAttribute createSettingAttribute(QLSSHCredentialInput sshCredentialInput, String accountId) {
    HostConnectionAttributes settingValue =
        createHostConnectionAttribute(accountId, sshCredentialInput.getAuthenticationScheme(),
            sshCredentialInput.getSshAuthentication(), sshCredentialInput.getKerberosAuthentication());
    return SettingAttribute.Builder.aSettingAttribute()
        .withName(sshCredentialInput.getName())
        .withValue(settingValue)
        .withAccountId(accountId)
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .withUsageRestrictions(
            usageScopeController.populateUsageRestrictions(sshCredentialInput.getUsageScope(), accountId))
        .build();
  }

  public SettingAttribute updateSSHCredential(QLSSHCredentialUpdate updateInput, String sshCredId, String accountId) {
    SettingAttribute existingSettingAttribute = settingsService.getByAccount(accountId, sshCredId);
    if (existingSettingAttribute == null
        || existingSettingAttribute.getValue().getSettingType() != HOST_CONNECTION_ATTRIBUTES) {
      throw new InvalidRequestException(String.format("No ssh credential exists with the id %s", sshCredId));
    }
    if (updateInput.getName().isPresent()) {
      String name = updateInput.getName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(name)) {
        throw new InvalidRequestException("Cannot set the ssh credential name as null");
      }
      existingSettingAttribute.setName(name);
    }
    boolean needToUpdateCred = false;
    QLSSHAuthenticationInput sshCredInput = null;
    QLKerberosAuthenticationInput kerberosInput = null;
    if (updateInput.getSshAuthentication().isPresent()) {
      sshCredInput = updateInput.getSshAuthentication().getValue().orElse(null);
      needToUpdateCred = true;
    }
    if (updateInput.getKerberosAuthentication().isPresent()) {
      kerberosInput = updateInput.getKerberosAuthentication().getValue().orElse(null);
      needToUpdateCred = true;
    }
    if (needToUpdateCred && (updateInput.getAuthenticationScheme() == null)) {
      throw new InvalidRequestException("The SSH authentication scheme is not provided with the update request");
    }
    if (needToUpdateCred) {
      HostConnectionAttributes sshSettings =
          createHostConnectionAttribute(accountId, updateInput.getAuthenticationScheme(), sshCredInput, kerberosInput);
      existingSettingAttribute.setValue(sshSettings);
    }
    if (updateInput.getUsageScope().isPresent()) {
      QLUsageScope usageScope = updateInput.getUsageScope().getValue().orElse(null);
      existingSettingAttribute.setUsageRestrictions(
          usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
    return settingsService.updateWithSettingFields(
        existingSettingAttribute, existingSettingAttribute.getUuid(), GLOBAL_APP_ID);
  }
}
