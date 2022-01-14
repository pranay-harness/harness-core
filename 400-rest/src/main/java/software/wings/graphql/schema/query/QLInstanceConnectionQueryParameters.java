package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDP)
public class QLInstanceConnectionQueryParameters implements QLPageQueryParameters {
  int limit;
  int offset;
  String environmentId;
  String serviceId;
  EnvironmentType envType;
  String accountId;
  DataFetchingFieldSelectionSet selectionSet;
  String applicationId;
}
