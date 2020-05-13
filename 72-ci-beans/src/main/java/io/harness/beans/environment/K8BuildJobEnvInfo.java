package io.harness.beans.environment;

import io.harness.beans.environment.pod.PodSetupInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores K8 specific data to setup Pod for running CI job
 */

@Data
@Value
@Builder
public class K8BuildJobEnvInfo implements BuildJobEnvInfo {
  @NotEmpty private PodsSetupInfo podsSetupInfo;
  @NotEmpty private String workDir;

  @Override
  public Type getType() {
    return Type.K8;
  }

  @Data
  @Builder
  public static final class PodsSetupInfo {
    private List<PodSetupInfo> podSetupInfoList = new ArrayList<>();
  }
}
