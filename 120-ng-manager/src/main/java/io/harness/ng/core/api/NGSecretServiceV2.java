package io.harness.ng.core.api;

import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface NGSecretServiceV2 {
  Optional<Secret> get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  Secret create(String accountIdentifier, SecretDTOV2 dto);

  boolean update(String accountIdentifier, SecretDTOV2 dto);

  SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretValidationMetaData metadata);

  Page<Secret> list(Criteria criteria, int page, int size);
}
