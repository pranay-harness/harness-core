/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secrets;

import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;
import static software.wings.settings.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLSecretQueryParameters;
import software.wings.graphql.schema.type.secrets.QLSecret;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class GetSecretDataFetcher extends AbstractObjectDataFetcher<QLSecret, QLSecretQueryParameters> {
  @Inject SecretManager secretManager;
  @Inject SettingsService settingsService;
  @Inject private WinRMCredentialController winRMCredentialController;
  @Inject private EncryptedTextController encryptedTextController;
  @Inject private SSHCredentialController sshCredentialController;
  @Inject private EncryptedFileController encryptedFileController;

  private InvalidRequestException throwInvalidSecretException() {
    throw new InvalidRequestException("No secret exists with given input.");
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLSecret fetch(QLSecretQueryParameters qlQuery, String accountId) {
    QLSecret qlSecret = null;
    if (qlQuery.getSecretType() == QLSecretType.ENCRYPTED_TEXT
        || qlQuery.getSecretType() == QLSecretType.ENCRYPTED_FILE) {
      qlSecret = getEncryptionSecret(accountId, qlQuery);
    }

    if (qlQuery.getSecretType() == QLSecretType.WINRM_CREDENTIAL
        || qlQuery.getSecretType() == QLSecretType.SSH_CREDENTIAL) {
      qlSecret = getWinrmOrSshSecret(accountId, qlQuery);
    }

    if (qlSecret == null || qlSecret.getSecretType() != qlQuery.getSecretType()) {
      throw throwInvalidSecretException();
    }
    return qlSecret;
  }

  private QLSecret getEncryptionSecret(String accountId, QLSecretQueryParameters qlQuery) {
    EncryptedData encryptedData;
    if (isNotBlank(qlQuery.getSecretId())) {
      encryptedData = secretManager.getSecretById(accountId, qlQuery.getSecretId());
    } else {
      encryptedData = secretManager.getSecretByName(accountId, qlQuery.getName());
    }
    QLSecret qlSecret = null;
    if (encryptedData != null) {
      // The secret is either encrypted Text or encrypted File, also ensuring that no other encrypted record is deleted
      if (encryptedData.getType() == SettingVariableTypes.SECRET_TEXT) {
        qlSecret = encryptedTextController.populateEncryptedText(encryptedData);
      } else if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
        qlSecret = encryptedFileController.populateEncryptedFile(encryptedData);
      } else {
        throw throwInvalidSecretException();
      }
    }
    return qlSecret;
  }

  private QLSecret getWinrmOrSshSecret(String accountId, QLSecretQueryParameters qlQuery) {
    QLSecret qlSecret = null;
    SettingAttribute settingAttribute = null;
    if (isNotBlank(qlQuery.getSecretId())) {
      settingAttribute = settingsService.get(qlQuery.getSecretId());
    } else {
      settingAttribute = settingsService.getSettingAttributeByName(accountId, qlQuery.getName());
    }
    // Since we did get without accountId, adding a check for accountId here.
    if (settingAttribute == null || !settingAttribute.getAccountId().equals(accountId)
        || settingAttribute.getCategory() != SETTING) {
      throw throwInvalidSecretException();
    }

    SettingValue settingValue = settingAttribute.getValue();

    if (settingValue != null
        && (settingValue.getSettingType() == HOST_CONNECTION_ATTRIBUTES
            || settingValue.getSettingType() == WINRM_CONNECTION_ATTRIBUTES)) {
      if (settingValue.getSettingType() == WINRM_CONNECTION_ATTRIBUTES) {
        qlSecret = winRMCredentialController.populateWinRMCredential(settingAttribute);
      } else {
        qlSecret = sshCredentialController.populateSSHCredential(settingAttribute);
      }
    } else {
      throw throwInvalidSecretException();
    }
    return qlSecret;
  }
}
