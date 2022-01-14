package io.harness.pms.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.Dependencies;

import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class DependenciesUtils {
  public Dependencies toDependenciesProto(Map<String, YamlField> fields) {
    if (EmptyPredicate.isEmpty(fields)) {
      return Dependencies.newBuilder().build();
    }

    Dependencies.Builder builder = Dependencies.newBuilder();
    fields.forEach((k, v) -> builder.putDependencies(k, v.getYamlPath()));
    return builder.build();
  }
}