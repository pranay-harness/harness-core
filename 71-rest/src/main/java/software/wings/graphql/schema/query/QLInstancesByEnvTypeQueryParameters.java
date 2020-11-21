package software.wings.graphql.schema.query;

import software.wings.beans.Environment.EnvironmentType;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLInstancesByEnvTypeQueryParameters implements QLPageQueryParameters {
  int limit;
  int offset;
  EnvironmentType envType;
  String accountId;
  DataFetchingFieldSelectionSet selectionSet;
}
