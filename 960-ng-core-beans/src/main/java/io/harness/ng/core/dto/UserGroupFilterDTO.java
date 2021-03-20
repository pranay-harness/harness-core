package io.harness.ng.core.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserGroupFilterDTO {
  Set<String> databaseIdFilter;
  Set<String> identifierFilter;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
}
