package software.wings.sm.states.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.k8s.model.KubernetesResource;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@JsonTypeName("k8sResources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sResourcesSweepingOutput implements SweepingOutput {
  public static final String K8S_RESOURCES_SWEEPING_OUTPUT = "k8sResources";
  private List<KubernetesResource> resources;
  private String manifests;
  private String stateType;

  @Override
  public String getType() {
    return K8S_RESOURCES_SWEEPING_OUTPUT;
  }
}
