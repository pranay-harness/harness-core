package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@Schema(name = "UserGroupFilter", description = "This is the view of the UserGroupFilter entity defined in Harness")
public class UserGroupFilterDTO {
  Set<String> databaseIdFilter;
  Set<String> identifierFilter;
  Set<String> userIdentifierFilter;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String searchTerm;
}
