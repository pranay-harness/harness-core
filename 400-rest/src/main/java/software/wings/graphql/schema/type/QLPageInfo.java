package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPageInfoKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLPageInfo implements QLObject {
  private Integer limit;
  private Integer offset;

  private Boolean hasMore;
  private Integer total;
}
