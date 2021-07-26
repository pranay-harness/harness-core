package software.wings.service.impl.instance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceInfoSummary {
    private String envId;
    private String lastArtifactBuildNum;
    private String serviceName;
    private String lastWorkflowExecutionId;
    private String lastWorkflowExecutionName;
    private String infraMappingId;
    private String infraMappingName;
}
