/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.validation.validators;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretText;
import io.harness.exception.SecretManagementException;
import io.harness.secrets.SecretsDao;
import io.harness.secrets.validation.BaseSecretValidator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@OwnedBy(PL)
public class VaultSecretManagerValidator extends BaseSecretValidator {
  @Inject
  public VaultSecretManagerValidator(SecretsDao secretsDao) {
    super(secretsDao);
  }

  @Override
  public void validateSecretText(String accountId, SecretText secretText, SecretManagerConfig secretManagerConfig) {
    super.validateSecretText(accountId, secretText, secretManagerConfig);
    if (secretText.isReferencedSecret()) {
      validatePath(secretText.getPath());
    }
  }

  @Override
  public void validateSecretTextUpdate(
      SecretText secretText, EncryptedData existingRecord, SecretManagerConfig secretManagerConfig) {
    super.validateSecretTextUpdate(secretText, existingRecord, secretManagerConfig);
    if (secretText.isReferencedSecret()) {
      validatePath(secretText.getPath());
    }
  }

  private void validatePath(String path) {
    if (path.indexOf('#') < 0) {
      throw new SecretManagementException(VAULT_OPERATION_ERROR,
          "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.", USER);
    }
  }
}
