package software.wings.service.impl.instance;

import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor
public final class ServiceAggregationInfo {
  @Id private ID _id;
  private EntitySummary appInfo;
  private EntitySummary infraMappingInfo;
  private EnvInfo envInfo;
  private ArtifactInfo artifactInfo;
  private List<EntitySummary> instanceInfoList;

  @Data
  @NoArgsConstructor
  public static final class ID {
    private String envId;
    private String lastArtifactId;
  }
}
