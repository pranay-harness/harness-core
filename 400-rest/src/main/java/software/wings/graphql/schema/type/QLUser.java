/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserKeys")
@Scope(ResourceType.USER)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUser implements QLObject {
  private String id;
  private String name;
  private String email;
  private Boolean isEmailVerified;
  private Boolean isTwoFactorAuthenticationEnabled;
  private Boolean isUserLocked;
  private Boolean isPasswordExpired;
  private Boolean isImportedFromIdentityProvider;
}
