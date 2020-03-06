package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.net.URI;

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
