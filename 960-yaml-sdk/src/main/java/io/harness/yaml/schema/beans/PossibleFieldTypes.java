package io.harness.yaml.schema.beans;

import java.util.Set;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PossibleFieldTypes {
  String fieldName;

  Set<SupportedPossibleFieldTypes> fieldTypes;
  /**
   * default field type if it is in any of fieldTypes.
   */
  @Nullable SupportedPossibleFieldTypes defaultFieldType;
}
