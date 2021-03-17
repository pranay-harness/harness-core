package io.harness.accesscontrol.clients;

import io.harness.scope.ResourceScope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "AccessControl")
@JsonTypeName("AccessControl")
public class HAccessControlDTO implements AccessControlDTO {
  String permission;
  ResourceScope resourceScope;
  String resourceType;
  String resourceIdentifier;
  boolean accessible;
}
