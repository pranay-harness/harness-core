package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Value;

@Value(staticConstructor = "of")
@TargetModule(Module._375_CE_GRAPHQL)
public class QLResourceEntry implements QLObject {
  String name;
  String quantity;
}
