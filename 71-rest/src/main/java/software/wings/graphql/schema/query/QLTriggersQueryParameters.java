package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
public class QLTriggersQueryParameters implements QLPageQueryParameters {
  private String applicationId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
