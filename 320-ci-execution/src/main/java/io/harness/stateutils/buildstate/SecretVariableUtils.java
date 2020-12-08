package io.harness.stateutils.buildstate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.beans.IdentifierRef;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.delegate.beans.ci.pod.SecretVariableDTO;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.exception.ngexception.CIStageExecutionUserException;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class SecretVariableUtils {
  private final SecretNGManagerClient secretNGManagerClient;
  private final SecretManagerClientService secretManagerClientService;

  @Inject
  public SecretVariableUtils(
      SecretNGManagerClient secretNGManagerClient, SecretManagerClientService secretManagerClientService) {
    this.secretNGManagerClient = secretNGManagerClient;
    this.secretManagerClientService = secretManagerClientService;
  }

  public SecretVariableDetails getSecretVariableDetails(NGAccess ngAccess, CustomSecretVariable secretVariable) {
    log.info("Getting secret variable details for secret ref [{}]", secretVariable.getValue().toSecretRefStringValue());
    SecretRefData secretRefData = secretVariable.getValue();
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(secretRefData.toSecretRefStringValue(),
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());

    SecretVariableDTO.Type secretType = getSecretType(getSecret(identifierRef).getType());
    SecretVariableDTO secret = SecretVariableDTO.builder()
                                   .name(secretVariable.getName())
                                   .secret(secretVariable.getValue())
                                   .type(secretType)
                                   .build();
    log.info("Getting secret variable encryption details for secret type:[{}] ref:[{}]", secretType,
        secretVariable.getValue().toSecretRefStringValue());
    List<EncryptedDataDetail> encryptionDetails = secretManagerClientService.getEncryptionDetails(ngAccess, secret);
    if (isEmpty(encryptionDetails)) {
      throw new InvalidArgumentsException("Secret encrypted details can't be empty or null", WingsException.USER);
    }

    return SecretVariableDetails.builder().encryptedDataDetailList(encryptionDetails).secretVariableDTO(secret).build();
  }

  private SecretVariableDTO.Type getSecretType(SecretType type) {
    switch (type) {
      case SecretFile:
        return SecretVariableDTO.Type.FILE;
      case SecretText:
        return SecretVariableDTO.Type.TEXT;
      default:
        throw new InvalidArgumentsException(format("Unsupported secret type [%s]", type), WingsException.USER);
    }
  }

  private SecretDTOV2 getSecret(IdentifierRef identifierRef) {
    SecretResponseWrapper secretResponseWrapper;
    try {
      secretResponseWrapper = SafeHttpCall
                                  .execute(secretNGManagerClient.getSecret(identifierRef.getIdentifier(),
                                      identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                                      identifierRef.getProjectIdentifier()))
                                  .getData();

    } catch (IOException e) {
      log.error(format("Unable to get secret information : [%s] with scope: [%s]", identifierRef.getIdentifier(),
          identifierRef.getScope()));

      throw new CIStageExecutionException(format("Unable to get secret information : [%s] with scope: [%s]",
                                              identifierRef.getIdentifier(), identifierRef.getScope()),
          e);
    }

    if (secretResponseWrapper == null) {
      throw new CIStageExecutionUserException(format("Secret not found for identifier : [%s] with scope: [%s]",
          identifierRef.getIdentifier(), identifierRef.getScope()));
    }
    return secretResponseWrapper.getSecret();
  }
}
