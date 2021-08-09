package io.harness.resourcegroup.framework.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@Builder
public class ResourceInfo {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String resourceIdentifier;
  String resourceType;
}
