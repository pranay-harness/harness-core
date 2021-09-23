package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDP)
public class QLClustersQueryParameters implements QLPageQueryParameters {
  private String accountId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
