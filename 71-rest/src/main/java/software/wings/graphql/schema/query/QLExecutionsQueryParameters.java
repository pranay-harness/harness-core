package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;
import software.wings.graphql.schema.type.QLExecutionStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Value
public class QLExecutionsQueryParameters implements QLPageQueryParameters {
  private String applicationId;
  private String pipelineId;
  private String workflowId;
  private String serviceId;

  private OffsetDateTime from;
  private OffsetDateTime to;

  private List<QLExecutionStatus> statuses;

  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
