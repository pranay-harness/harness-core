/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
