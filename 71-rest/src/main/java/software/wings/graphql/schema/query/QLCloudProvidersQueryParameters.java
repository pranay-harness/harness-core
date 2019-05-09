package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
public class QLCloudProvidersQueryParameters implements QLPageQueryParameters {
  private String accountId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
