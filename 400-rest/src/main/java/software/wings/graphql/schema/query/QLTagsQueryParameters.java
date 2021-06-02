package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLTagsQueryParameters implements EntityFilter {
  String serviceId;
  String envId;
  String workflowId;
  String pipelineId;
  String triggerId;
  String applicationId;
}
