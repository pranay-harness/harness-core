package io.harness.beans.environment.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Stores all details require to spawn pod
 */

@Data
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PodSetupInfo {
  private PodSetupParams podSetupParams;
  @NotEmpty private String name;

  @Data
  @Builder
  public static final class PodSetupParams {
    private List<ContainerDefinitionInfo> containerDefinitionInfos;
  }
}
