package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
public class QLExecutionsParameters implements QLPageQueryParameters {
  private String appId;
  private String pipelineId;
  private String workflowId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
