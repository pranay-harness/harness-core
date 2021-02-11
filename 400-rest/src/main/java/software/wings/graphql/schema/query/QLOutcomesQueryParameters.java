package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Value;

@Value
@TargetModule(Module._380_CG_GRAPHQL)
public class QLOutcomesQueryParameters {
  private String executionId;

  private DataFetchingFieldSelectionSet selectionSet;

  public boolean isServiceRequested() {
    // TODO: it is not trivial how to find out if a field in inline fragment is selected
    return true;
  }

  public boolean isEnvironmentRequested() {
    // TODO: it is not trivial how to find out if a field in inline fragment is selected
    return true;
  }
}
