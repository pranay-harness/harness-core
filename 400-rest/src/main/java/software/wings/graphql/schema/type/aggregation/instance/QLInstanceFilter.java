/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation.instance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;
import software.wings.graphql.schema.type.aggregation.service.QLDeploymentTypeFilter;
import software.wings.graphql.schema.type.aggregation.workflow.QLOrchestrationWorkflowTypeFilter;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLInstanceFilter implements EntityFilter {
  private QLTimeFilter createdAt;
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter environment;
  private QLIdFilter cloudProvider;
  private QLInstanceType instanceType;
  private QLInstanceTagFilter tag;
  private QLEnvironmentTypeFilter environmentType;
  private QLDeploymentTypeFilter deploymentType;
  private QLOrchestrationWorkflowTypeFilter orchestrationWorkflowType;
  private QLIdFilter workflow;
  private QLIdFilter lastWorkflowExecutionId;
}
