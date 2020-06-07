package software.wings.service.impl.instance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;

@Data
@NoArgsConstructor
public final class AggregationInfo {
  @Id private ID _id;
  private long count;
  private EntitySummary appInfo;
  private EntitySummary serviceInfo;
  private EntitySummary infraMappingInfo;
  private EnvInfo envInfo;
  private ArtifactInfo artifactInfo;
  private List<EntitySummary> instanceInfoList;

  @Data
  @NoArgsConstructor
  public static final class ID {
    private String serviceId;
    private String envId;
    private String lastArtifactId;
  }
}
