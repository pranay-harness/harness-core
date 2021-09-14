/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLChangeDetails implements QLObject {
  private String resourceId;
  private String resourceType;
  private String resourceName;
  private String operationType;
  private Boolean failure;
  private String appId;
  private String appName;
  private String parentResourceId;
  private String parentResourceName;
  private String parentResourceType;
  private String parentResourceOperation;
  private Long createdAt;
}
