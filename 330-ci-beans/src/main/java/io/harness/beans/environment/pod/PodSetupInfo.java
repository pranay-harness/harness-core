package io.harness.beans.environment.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.delegate.beans.ci.pod.PVCParams;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.validation.constraints.NotNull;

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
  @NotNull private Integer stageMemoryRequest;
  @NotNull private Integer stageCpuRequest;
  private List<String> serviceIdList;
  private List<Integer> serviceGrpcPortList;

  @Data
  @Builder
  public static final class PodSetupParams {
    private List<ContainerDefinitionInfo> containerDefinitionInfos;
  }
}
