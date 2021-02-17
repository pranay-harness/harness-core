package io.harness.accesscontrol;

import io.harness.accesscontrol.principals.PrincipalType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "PrincipalKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HPrincipal implements Principal {
  String principalIdentifier;
  PrincipalType principalType;
}
