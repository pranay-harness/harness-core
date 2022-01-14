package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.helm.HelmChartInfo;

import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor
@OwnedBy(DX)
public final class AggregationInfo {
  @Id private ID _id;
  private long count;
  private EntitySummary appInfo;
  private EntitySummary serviceInfo;
  private EntitySummary infraMappingInfo;
  private EnvInfo envInfo;
  private ArtifactInfo artifactInfo;
  private HelmChartInfo helmChartInfo;
  private List<EntitySummary> instanceInfoList;

  @Data
  @NoArgsConstructor
  public static final class ID {
    private String serviceId;
    private String envId;
    private String lastArtifactId;
  }
}
