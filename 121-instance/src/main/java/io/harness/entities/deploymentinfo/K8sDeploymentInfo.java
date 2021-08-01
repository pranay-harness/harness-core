package io.harness.entities.deploymentinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashSet;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.DX)
public class K8sDeploymentInfo extends DeploymentInfo {
    @NotNull private LinkedHashSet<String> namespaces;
    @NotNull private String releaseName;
    private String blueGreenStageColor;
}
