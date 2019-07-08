package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPageQueryParameterImpl implements QLPageQueryParameters {
  private int limit;
  private int offset;
  private DataFetchingFieldSelectionSet selectionSet;
}
