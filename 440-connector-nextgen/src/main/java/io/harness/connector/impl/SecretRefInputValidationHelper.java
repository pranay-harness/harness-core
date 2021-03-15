package io.harness.connector.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class SecretRefInputValidationHelper {
  private SecretCrudService secretCrudService;

  public void validateTheSecretInput(SecretRefData secretRefData, NGAccess ngAccess) {
    if (secretRefData == null) {
      return;
    }
    IdentifierRef secretIdentifiers = IdentifierRefHelper.getIdentifierRef(secretRefData.toSecretRefStringValue(),
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    validateTheScopeOfTheSecret(secretRefData, secretIdentifiers);
    validateTheSecretIsPresent(secretRefData, secretIdentifiers);
  }

  public Set<SecretRefData> getDecryptableFieldsData(List<DecryptableEntity> decryptableEntities) {
    Set<SecretRefData> secrets = new HashSet<>();

    for (DecryptableEntity decryptableEntity : decryptableEntities) {
      List<Field> secretFields = decryptableEntity.getSecretReferenceFields();
      for (Field secretField : secretFields) {
        SecretRefData secretRefData = null;
        try {
          secretField.setAccessible(true);
          secretRefData = (SecretRefData) secretField.get(decryptableEntity);
        } catch (IllegalAccessException ex) {
          log.info("Error reading the secret data", ex);
          throw new UnexpectedException("Error processing the data");
        }
        secrets.add(secretRefData);
      }
    }
    return secrets;
  }

  private void validateTheScopeOfTheSecret(SecretRefData secretRefData, NGAccess ngAccess) {
    if (isNotBlank(ngAccess.getProjectIdentifier())) {
      // It is a project level entity
      return;
    } else if (isNotBlank(ngAccess.getOrgIdentifier())) {
      // It is a org level entity
      if (secretRefData.getScope() == Scope.PROJECT) {
        throw new InvalidRequestException("The project level secret cannot be used at a org level");
      }
    } else {
      // It is a account level entity
      if (secretRefData.getScope() == Scope.PROJECT || secretRefData.getScope() == Scope.ORG) {
        throw new InvalidRequestException(String.format(
            "The %s level secret cannot be used at account level", secretRefData.getScope().getYamlRepresentation()));
      }
    }
  }

  private void validateTheSecretIsPresent(SecretRefData secretRefData, NGAccess ngAccess) {
    Optional<SecretResponseWrapper> secretResponseWrapper = secretCrudService.get(ngAccess.getAccountIdentifier(),
        ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier(), secretRefData.getIdentifier());
    if (secretResponseWrapper == null || !secretResponseWrapper.isPresent()) {
      String projectScopeString =
          isNotBlank(ngAccess.getProjectIdentifier()) ? " in project " + ngAccess.getProjectIdentifier() : "";
      String orgScopeString =
          isNotBlank(ngAccess.getOrgIdentifier()) ? " in organization " + ngAccess.getOrgIdentifier() : "";
      throw new InvalidRequestException(String.format(
          "No secret exists with the id %s %s", secretRefData.getIdentifier(), orgScopeString + projectScopeString));
    }
  }
}
