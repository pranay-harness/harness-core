package io.harness.yaml.schema.beans;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwaggerDefinitionsMetaInfo {
  Set<FieldSubtypeData> subtypeClassMap;
  Set<OneOfMapping> oneOfMappings;
  Set<PossibleFieldTypes> fieldPossibleTypes;
}
