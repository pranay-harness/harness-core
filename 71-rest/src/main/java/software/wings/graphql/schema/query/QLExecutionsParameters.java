package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;
import software.wings.graphql.schema.type.QLExecutionStatus;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class QLExecutionsParameters implements QLPageQueryParameters {
  private String applicationId;
  private String pipelineId;
  private String workflowId;
  private String serviceId;

  private ZonedDateTime from;
  private ZonedDateTime to;

  private List<QLExecutionStatus> statuses;

  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
