package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstancesByEnvironmentQueryParameters implements QLPageQueryParameters {
  int limit;
  int offset;
  String envId;
  DataFetchingFieldSelectionSet selectionSet;
}
