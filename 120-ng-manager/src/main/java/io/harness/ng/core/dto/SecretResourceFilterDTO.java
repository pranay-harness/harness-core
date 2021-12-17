package io.harness.ng.core.dto;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCategory;
import io.harness.secretmanagerclient.SecretType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@Schema(name = "SecretResourceFilter", description = "This has details of the Secret Filter in Harness")
public class SecretResourceFilterDTO {
  @Schema(description = NGResourceFilterConstants.IDENTIFIER_LIST) List<String> identifiers;
  @Schema(description = NGResourceFilterConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = NGResourceFilterConstants.TYPE_LIST) List<SecretType> secretTypes;
  ConnectorCategory sourceCategory;
  @Schema(description = "Boolean value to indicate if Secrets are filtered from every child scope.")
  boolean includeSecretsFromEverySubScope;
}
