package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_RECOMMENDATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUnitPrice {
  BigDecimal cpu;
  BigDecimal memory;
  @Builder.Default String info = "cpu:'cost of 1 CPU/hr',  memory:'cost of 1GB Memory/hr'";
}
