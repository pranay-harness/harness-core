package io.harness.scim;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScimMultiValuedObject<T> {
  String type;
  boolean primary;
  String display;
  T value;
  @JsonProperty("$ref") URI ref;

  String id;
  String displayName;
  boolean active;
}
