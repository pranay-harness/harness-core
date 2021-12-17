package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterConstants;

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
@Schema(name = "ServiceAccountFilter",
    description = "This has the details of the Service Account Filter as defined in Harness")
public class ServiceAccountFilterDTO {
  @Schema(description = FilterConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountIdentifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier;
  @Schema(description = FilterConstants.IDENTIFIER_LIST) List<String> identifiers;
}
