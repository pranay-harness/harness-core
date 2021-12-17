package io.harness.ng.core.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorCategory;
import io.harness.filter.FilterConstants;
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
  @Schema(description = FilterConstants.IDENTIFIER_LIST) List<String> identifiers;
  @Schema(description = FilterConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = FilterConstants.TYPE_LIST) List<SecretType> secretTypes;
  ConnectorCategory sourceCategory;
  @Schema(description = "Boolean value to indicate if Secrets are filtered from every child scope.")
  boolean includeSecretsFromEverySubScope;
}
