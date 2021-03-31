package software.wings.graphql.datafetcher.ce.recommendation.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_RECOMMENDATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
public class QLK8SWorkloadHistogramData {
  List<QLContainerHistogramData> containerHistogramDataList;
}
