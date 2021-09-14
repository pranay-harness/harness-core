/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.mutation.artifact;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class ArtifactCleanUpPayload {
  private String message;
}
