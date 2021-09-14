/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCategory;
import io.harness.secretmanagerclient.SecretType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class SecretResourceFilterDTO {
  List<String> identifiers;
  String searchTerm;
  List<SecretType> secretTypes;
  ConnectorCategory sourceCategory;
  boolean includeSecretsFromEverySubScope;
}
