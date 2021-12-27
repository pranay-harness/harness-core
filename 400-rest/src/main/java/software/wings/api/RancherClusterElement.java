package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RancherClusterElement implements ContextElement {

    private String uuid;
    private String clusterName;

    @Override
    public ContextElementType getElementType() {
        return ContextElementType.RANCHER_K8S_CLUSTER_CRITERIA;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public String getName() {
        return this.clusterName;
    }

    @Override
    public Map<String, Object> paramMap(ExecutionContext context) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(INSTANCE, this);

        return paramMap;
    }

    @Override
    public ContextElement cloneMin() {
        return new RancherClusterElement(this.uuid, this.clusterName);
    }
}