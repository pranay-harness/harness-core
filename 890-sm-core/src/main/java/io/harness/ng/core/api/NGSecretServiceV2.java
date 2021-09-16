package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NGSecretServiceV2 {
  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  Optional<Secret> get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  Secret create(String accountIdentifier, SecretDTOV2 dto, boolean draft);

  Secret update(String accountIdentifier, SecretDTOV2 dto, boolean draft);

  SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretValidationMetaData metadata);

  long count(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Page<Secret> list(Criteria criteria, int page, int size);
}
