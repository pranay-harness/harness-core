/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secrets;

import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;
import static software.wings.graphql.schema.type.secrets.QLSecretType.ENCRYPTED_FILE;
import static software.wings.graphql.schema.type.secrets.QLSecretType.ENCRYPTED_TEXT;
import static software.wings.graphql.schema.type.secrets.QLSecretType.SSH_CREDENTIAL;
import static software.wings.graphql.schema.type.secrets.QLSecretType.WINRM_CREDENTIAL;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.settings.SettingVariableTypes.CONFIG_FILE;
import static software.wings.settings.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingVariableTypes.SECRET_TEXT;
import static software.wings.settings.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.secrets.input.QLDeleteSecretInput;
import software.wings.graphql.schema.mutation.secrets.payload.QLDeleteSecretPayload;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.security.auth.SecretAuthHandler;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DeleteSecretDataFetcher extends BaseMutatorDataFetcher<QLDeleteSecretInput, QLDeleteSecretPayload> {
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private SecretAuthHandler secretAuthHandler;

  @Inject
  public DeleteSecretDataFetcher() {
    super(QLDeleteSecretInput.class, QLDeleteSecretPayload.class);
  }

  private void throwInvalidSecretException(String secretId) {
    throw new InvalidRequestException(String.format("No secret exists with the id %s", secretId));
  }

  private void deleteTextOrFileSecret(String accountId, String secretId, QLSecretType inputSecretType) {
    EncryptedData encryptedData = secretManager.getSecretById(accountId, secretId);
    if (encryptedData != null) {
      if (encryptedData.getType() == SECRET_TEXT && inputSecretType == ENCRYPTED_TEXT
          || encryptedData.getType() == CONFIG_FILE && inputSecretType == ENCRYPTED_FILE) {
        secretManager.deleteSecret(accountId, secretId, new HashMap<>(), true);
      } else {
        throwInvalidSecretException(secretId);
      }
    } else {
      throwInvalidSecretException(secretId);
    }
  }

  private void deleteConnectionSecrets(String accountId, String secretId, QLSecretType inputSecretType) {
    SettingAttribute settingAttribute = settingsService.getByAccount(accountId, secretId);
    if (settingAttribute == null || settingAttribute.getCategory() != SETTING) {
      throwInvalidSecretException(secretId);
    }
    SettingValue settingValue = null;
    if (settingAttribute != null) {
      settingValue = settingAttribute.getValue();
    }
    if (settingValue != null) {
      if ((settingValue.getSettingType() == HOST_CONNECTION_ATTRIBUTES && inputSecretType == SSH_CREDENTIAL)
          || (settingValue.getSettingType() == WINRM_CONNECTION_ATTRIBUTES && inputSecretType == WINRM_CREDENTIAL)) {
        settingsService.delete(null, secretId);
      } else {
        throwInvalidSecretException(secretId);
      }
    } else {
      throwInvalidSecretException(secretId);
    }
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLDeleteSecretPayload mutateAndFetch(QLDeleteSecretInput input, MutationContext mutationContext) {
    String secretId = input.getSecretId();
    String accountId = mutationContext.getAccountId();
    if (isBlank(secretId)) {
      throw new InvalidRequestException("The secretId cannot be null in the delete request");
    }
    QLSecretType inputSecretType = input.getSecretType();
    if (inputSecretType == ENCRYPTED_TEXT || inputSecretType == ENCRYPTED_FILE) {
      secretAuthHandler.authorize();
      deleteTextOrFileSecret(accountId, secretId, inputSecretType);
    } else if (inputSecretType == WINRM_CREDENTIAL || inputSecretType == SSH_CREDENTIAL) {
      deleteConnectionSecrets(accountId, secretId, inputSecretType);
    } else {
      throw new UnexpectedException("Invalid secretType provided in the request");
    }
    return QLDeleteSecretPayload.builder().clientMutationId(input.getClientMutationId()).build();
  }
}
