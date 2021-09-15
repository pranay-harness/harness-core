/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCategory;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.remote.SecretValidationMetaData;
import io.harness.ng.core.remote.SecretValidationResultDTO;
import io.harness.secretmanagerclient.SecretType;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@OwnedBy(PL)
public interface SecretCrudService {
  Boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretResponseWrapper create(String accountIdentifier, SecretDTOV2 dto);

  SecretResponseWrapper createViaYaml(String accountIdentifier, SecretDTOV2 dto);

  Optional<SecretResponseWrapper> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  PageResponse<SecretResponseWrapper> list(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      List<String> identifiers, List<SecretType> secretTypes, boolean includeSecretsFromEverySubScope,
      String searchTerm, int page, int size, ConnectorCategory sourceCategory);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretResponseWrapper createFile(String accountIdentifier, SecretDTOV2 dto, InputStream inputStream);

  SecretResponseWrapper updateFile(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretDTOV2 updateDTO, InputStream inputStream);

  SecretResponseWrapper update(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretDTOV2 updateDTO);

  SecretResponseWrapper updateViaYaml(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretDTOV2 updateDTO);

  SecretValidationResultDTO validateSecret(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretValidationMetaData metadata);
}
