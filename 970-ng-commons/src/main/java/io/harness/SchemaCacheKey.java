package io.harness;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchemaCacheKey {
  String accountIdentifier;
  ModuleType moduleType;
}
