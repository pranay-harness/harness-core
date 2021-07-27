package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDC)
public class QLEnvironmentsQueryParameters implements QLPageQueryParameters {
  private String applicationId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
