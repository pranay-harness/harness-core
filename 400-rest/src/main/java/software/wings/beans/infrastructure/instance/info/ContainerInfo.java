package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Base class for container instance like docker
 * @author rktummala on 08/25/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = KubernetesContainerInfo.class, name = "KUBERNETES_CONTAINER_INFO")
  , @JsonSubTypes.Type(value = EcsContainerInfo.class, name = "ECS_CONTAINER_INFO"),
      @JsonSubTypes.Type(value = K8sPodInfo.class, name = "K8S_POD_INFO")
})
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public abstract class ContainerInfo extends InstanceInfo {
  private String clusterName;

  public ContainerInfo(String clusterName) {
    this.clusterName = clusterName;
  }
}
