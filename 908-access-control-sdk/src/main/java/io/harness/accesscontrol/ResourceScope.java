package io.harness.accesscontrol;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder(builderClassName = "Builder")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceScope {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
}
