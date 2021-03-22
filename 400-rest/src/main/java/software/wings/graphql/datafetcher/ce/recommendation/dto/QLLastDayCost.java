package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_RECOMMENDATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLLastDayCost {
  BigDecimal cpu;
  BigDecimal memory;
  @Builder.Default
  String info = "cpu:'Last day's total CPU Cost' and memory:'Last day's total Memory Cost' for this workload.";
}
