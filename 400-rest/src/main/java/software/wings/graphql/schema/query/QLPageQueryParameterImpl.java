package software.wings.graphql.schema.query;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLPageQueryParameterImpl implements QLPageQueryParameters {
  private int limit;
  private int offset;
  private DataFetchingFieldSelectionSet selectionSet;
  private DataFetchingEnvironment dataFetchingEnvironment;
}
