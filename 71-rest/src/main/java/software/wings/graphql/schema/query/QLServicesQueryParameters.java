package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
public class QLServicesQueryParameters implements QLPageQueryParameters {
  private String appId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
