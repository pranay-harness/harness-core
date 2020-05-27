package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagFilter;

@OwnedBy(CDC)
@Value
@Builder
@ToString
public class QLExecutionFilter implements EntityFilter {
  private QLIdFilter execution;
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter cloudProvider;
  private QLIdFilter environment;
  private QLStringFilter status;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;
  private QLNumberFilter duration;
  private QLIdFilter triggeredBy;
  private QLIdFilter trigger;
  private QLIdFilter workflow;
  private QLIdFilter pipeline;
  private QLTimeFilter creationTime;
  private QLIdFilter pipelineExecution;
  private QLDeploymentTagFilter tag;
}
