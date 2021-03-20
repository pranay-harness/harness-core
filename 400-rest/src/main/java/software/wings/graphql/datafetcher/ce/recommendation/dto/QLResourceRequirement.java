package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLResourceRequirement implements QLObject {
  String yaml;
  @Singular List<QLResourceEntry> requests;
  @Singular List<QLResourceEntry> limits;
}
