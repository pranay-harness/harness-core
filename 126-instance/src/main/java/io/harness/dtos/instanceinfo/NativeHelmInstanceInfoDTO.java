package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.k8s.model.HelmVersion;
import io.harness.util.InstanceSyncKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NativeHelmInstanceInfoDTO extends InstanceInfoDTO {

    @NotNull private String podName;
    private String ip;
    private String namespace;
    private String releaseName;
    private HelmChartInfo helmChartInfo;
    private HelmVersion helmVersion;

    @Override
    public String prepareInstanceKey() {
        return InstanceSyncKey.builder()
                .clazz(NativeHelmInstanceInfoDTO.class)
                .part(podName)
                .part(ip)
                .part(namespace)
                .build()
                .toString();
    }

    @Override
    public String prepareInstanceSyncHandlerKey() {
        return InstanceSyncKey.builder().part(releaseName).build().toString();
    }
}
