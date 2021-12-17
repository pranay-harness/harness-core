package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.ApiKeyType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "TokenFilter", description = "This has the details of the Token Filter as defined in Harness")
public class TokenFilterDTO {
  @Schema(description = NGResourceFilterConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountIdentifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier;
  String parentIdentifier;
  ApiKeyType apiKeyType;
  String apiKeyIdentifier;
  @Schema(description = NGResourceFilterConstants.IDENTIFIER_LIST) List<String> identifiers;
}
