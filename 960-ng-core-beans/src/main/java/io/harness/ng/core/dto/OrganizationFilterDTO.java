package io.harness.ng.core.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationFilterDTO {
  String searchTerm;
  List<String> identifiers;
  boolean ignoreCase;
}
