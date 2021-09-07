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
