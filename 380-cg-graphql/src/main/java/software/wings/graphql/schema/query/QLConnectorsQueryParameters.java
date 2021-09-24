package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDP)
public class QLConnectorsQueryParameters implements QLPageQueryParameters {
  private String accountId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
