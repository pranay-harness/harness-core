package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_RECOMMENDATION)
public class QLK8sWorkloadRecommendation {
  String namespace;
  String workloadType;
  String workloadName;
  @Singular List<QLContainerRecommendation> containerRecommendations;
  Double estimatedSavings;
}
