package software.wings.graphql.datafetcher.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagFilter;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QLExecutionFilter extends QLBaseExecutionFilter {
  QLIdFilter pipelineExecutionId;

  @Builder
  public QLExecutionFilter(QLIdFilter execution, QLIdFilter application, QLIdFilter service, QLIdFilter cloudProvider,
      QLIdFilter environment, QLIdFilter status, QLTimeFilter endTime, QLTimeFilter startTime, QLNumberFilter duration,
      QLIdFilter triggeredBy, QLIdFilter trigger, QLIdFilter workflow, QLIdFilter pipeline, QLTimeFilter creationTime,
      QLDeploymentTagFilter tag, QLIdFilter pipelineExecutionId) {
    super(execution, application, service, cloudProvider, environment, status, endTime, startTime, duration,
        triggeredBy, trigger, workflow, pipeline, creationTime, tag);
    this.pipelineExecutionId = pipelineExecutionId;
  }
}
