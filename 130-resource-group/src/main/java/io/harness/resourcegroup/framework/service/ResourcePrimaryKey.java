package io.harness.resourcegroup.framework.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@Builder
public class ResourcePrimaryKey {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifer;
  String resourceIdetifier;
  String resourceType;
}
