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
import software.wings.graphql.schema.type.aggregation.service.QLWorkflowTypeFilter;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
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
  private QLWorkflowTypeFilter workflowType;
  private QLIdFilter workflow;
  private QLIdFilter lastWorkflowExecutionId;
}
