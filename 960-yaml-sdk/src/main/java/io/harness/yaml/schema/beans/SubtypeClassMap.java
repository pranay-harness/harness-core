package io.harness.yaml.schema.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class SubtypeClassMap {
  /**
   * The subtype field value which will determine to get into current condition.
   */
  String subtypeEnum;
  /**
   * The definition key to which $ref will point.
   */
  String subTypeDefinitionKey;
}