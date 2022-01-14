package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppFilterKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAppFilter {
  private QLAppFilterType filterType;
  private Set<String> appIds;
}
