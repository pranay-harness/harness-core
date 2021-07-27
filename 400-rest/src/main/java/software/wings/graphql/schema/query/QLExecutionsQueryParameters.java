package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLExecutionStatus;

import graphql.schema.DataFetchingFieldSelectionSet;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDC)
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
