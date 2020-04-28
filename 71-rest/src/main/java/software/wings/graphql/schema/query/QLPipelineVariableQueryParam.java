package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLPipelineVariableQueryParam {
  String pipelineId;
  String applicationId;
}
