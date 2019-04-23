package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.beans.Environment.EnvironmentType;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstancesByEnvTypeQueryParameters implements QLPageQueryParameters {
  int limit;
  int offset;
  EnvironmentType envType;
  DataFetchingFieldSelectionSet selectionSet;
}
