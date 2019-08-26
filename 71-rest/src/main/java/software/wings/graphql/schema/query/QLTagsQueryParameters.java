package software.wings.graphql.schema.query;

import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;

@Value
public class QLTagsQueryParameters implements EntityFilter {
  String serviceId;
  String envId;
  String workflowId;
  String pipelineId;
  String triggerId;
  String applicationId;
}
