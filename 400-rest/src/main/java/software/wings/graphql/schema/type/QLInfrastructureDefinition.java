/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type;

import software.wings.beans.Environment;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLInfrastructureDefinition implements QLObject {
  private String id;
  private String name;
  private String deploymentType;
  private Environment environment;
  private Long createdAt;
  private List<String> scopedToServices;
}
