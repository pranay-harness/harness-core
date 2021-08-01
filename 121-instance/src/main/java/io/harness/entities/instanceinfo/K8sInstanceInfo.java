package io.harness.entities.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.K8sContainer;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class K8sInstanceInfo extends InstanceInfo {
    @NotNull
    private String namespace;
    @NotNull private String releaseName;
    @NotNull private String podName;
    private String podIP;
    private String blueGreenColor;
    @NotNull private List<K8sContainer> containerList;
}
