package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public class QLEnvironmentsQueryParameters implements QLPageQueryParameters {
  private String applicationId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
