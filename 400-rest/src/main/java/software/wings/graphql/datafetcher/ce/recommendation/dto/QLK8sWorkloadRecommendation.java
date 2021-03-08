package software.wings.graphql.datafetcher.ce.recommendation.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_RECOMMENDATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLK8sWorkloadRecommendation {
  String namespace;
  String workloadType;
  String workloadName;
  String clusterId;
  String clusterName;
  QLLastDayCost lastDayCost;
  @Singular List<QLContainerRecommendation> containerRecommendations;
  BigDecimal estimatedSavings;
  int numDays;
  QLK8sWorkloadRecommendationPreset preset;
}
