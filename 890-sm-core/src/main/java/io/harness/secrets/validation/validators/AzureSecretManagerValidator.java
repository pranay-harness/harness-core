/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.validation.validators;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER_SRE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretFile;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretText;
import io.harness.exception.SecretManagementException;
import io.harness.secrets.SecretsDao;
import io.harness.secrets.validation.BaseSecretValidator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.regex.Pattern;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@OwnedBy(PL)
public class AzureSecretManagerValidator extends BaseSecretValidator {
  private static final Pattern AZURE_KEY_VAULT_NAME_PATTERN = Pattern.compile("^[\\da-zA-Z-]+$");
  private static final int AZURE_SECRET_CONTENT_SIZE_LIMIT = 24000;

  @Inject
  public AzureSecretManagerValidator(SecretsDao secretsDao) {
    super(secretsDao);
  }

  private void validateSecretName(String name) {
    if (!AZURE_KEY_VAULT_NAME_PATTERN.matcher(name).find()) {
      String message = "Secret name can only contain alphanumeric characters, or -";
      throw new SecretManagementException(AZURE_KEY_VAULT_OPERATION_ERROR, message, USER_SRE);
    }
  }

  private void verifyFileSizeWithinLimit(byte[] fileContent) {
    if (isNotEmpty(fileContent) && fileContent.length > AZURE_SECRET_CONTENT_SIZE_LIMIT) {
      String message = "Azure Secrets Manager limits secret value to " + AZURE_SECRET_CONTENT_SIZE_LIMIT + " bytes.";
      throw new SecretManagementException(AZURE_KEY_VAULT_OPERATION_ERROR, message, USER_SRE);
    }
  }

  @Override
  public void validateSecretText(String accountId, SecretText secretText, SecretManagerConfig secretManagerConfig) {
    super.validateSecretText(accountId, secretText, secretManagerConfig);
    if (secretText.isInlineSecret()) {
      validateSecretName(secretText.getName());
    }
  }

  @Override
  public void validateSecretTextUpdate(
      SecretText secretText, EncryptedData existingRecord, SecretManagerConfig secretManagerConfig) {
    super.validateSecretTextUpdate(secretText, existingRecord, secretManagerConfig);
    if (secretText.isInlineSecret()) {
      validateSecretName(secretText.getName());
    }
  }

  @Override
  public void validateSecretFile(String accountId, SecretFile secretFile, SecretManagerConfig secretManagerConfig) {
    super.validateSecretFile(accountId, secretFile, secretManagerConfig);
    validateSecretName(secretFile.getName());
    verifyFileSizeWithinLimit(secretFile.getFileContent());
  }

  @Override
  public void validateSecretFileUpdate(
      SecretFile secretFile, EncryptedData existingRecord, SecretManagerConfig secretManagerConfig) {
    super.validateSecretFileUpdate(secretFile, existingRecord, secretManagerConfig);
    validateSecretName(secretFile.getName());
    verifyFileSizeWithinLimit(secretFile.getFileContent());
  }
}
