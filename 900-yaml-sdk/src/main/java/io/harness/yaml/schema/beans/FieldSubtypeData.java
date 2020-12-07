package io.harness.yaml.schema.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FieldSubtypeData {
  /**
   * Field name in jsonSchema on which {@link JsonSubTypes} is applied.
   */
  String fieldName;
  /**
   * name of discriminator on which the subtype resolution depends.
   */
  String discriminatorName;
  /**
   * Type of discriminator.
   */
  JsonTypeInfo.As discriminatorType;
  /**
   * Mapping of subTypes.
   */
  Set<SubtypeClassMap> subtypesMapping;
}