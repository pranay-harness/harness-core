package software.wings.service.impl.instance;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.helm.HelmChartInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.DX;

@Data
@NoArgsConstructor
@OwnedBy(DX)
public class TemporaryTestInfo {
    @Id
    private AggregationInfo.ID _id;
    private EntitySummary serviceInfo;
    private EntitySummary infraMappingInfo;
    private EnvInfo envInfo;
    private ArtifactInfo artifactInfo;
    private HelmChartInfo helmChartInfo;
    private List<EntitySummary> instanceInfoList;
    private List<ServiceInfoSummary> serviceInfoSummaries;

    @Data
    @NoArgsConstructor
    public static final class ID {
        private String serviceId;
        private String envId;
        private String lastArtifactBuildNum;
    }

    @Data
    @NoArgsConstructor
    public static final class ServiceInfoSummary {
        private String serviceId;
        private String envId;
        private String lastArtifactBuildNum;
    }

}
