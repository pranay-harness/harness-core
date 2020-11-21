package software.wings.graphql.schema.query;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import lombok.Value;

@Value
public class QLTagsQueryParameters implements EntityFilter {
  String serviceId;
  String envId;
  String workflowId;
  String pipelineId;
  String triggerId;
  String applicationId;
}
