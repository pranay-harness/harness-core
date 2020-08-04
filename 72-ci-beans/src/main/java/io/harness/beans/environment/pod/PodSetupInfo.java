package io.harness.beans.environment.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ci.pod.PVCParams;

import java.util.List;

/**
 * Stores all details require to spawn pod
 */

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PodSetupInfo {
  private PodSetupParams podSetupParams;
  private PVCParams pvcParams;
  @NotEmpty private String name;

  @Data
  @Builder
  public static final class PodSetupParams {
    private List<ContainerDefinitionInfo> containerDefinitionInfos;
  }
}
