package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Value;

@Value(staticConstructor = "of")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLResourceEntry implements QLObject {
  String name;
  String quantity;
}
