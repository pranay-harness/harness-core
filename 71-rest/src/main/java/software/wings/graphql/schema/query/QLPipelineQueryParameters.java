package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipelineQueryParameters {
  private String pipelineId;
  private String executionId;
  private String pipelineName;
  private String applicationId;
}
