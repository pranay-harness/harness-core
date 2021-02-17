package io.harness.accesscontrol.acl.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HAccessControlDTO implements AccessControlDTO {
  String permission;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String resourceIdentifier;
  boolean hasAccess;
}
