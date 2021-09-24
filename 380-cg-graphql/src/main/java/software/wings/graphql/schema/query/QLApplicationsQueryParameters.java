package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLApplicationsQueryParameters implements QLPageQueryParameters {
  private String accountId;
  private int limit;
  private int offset;

  private DataFetchingFieldSelectionSet selectionSet;
}
