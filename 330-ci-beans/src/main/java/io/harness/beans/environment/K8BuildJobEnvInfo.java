package io.harness.beans.environment;

import io.harness.beans.environment.pod.PodSetupInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Stores K8 specific data to setup Pod for running CI job
 */

@Data
@Value
@Builder
public class K8BuildJobEnvInfo implements BuildJobEnvInfo {
  @NotEmpty private PodsSetupInfo podsSetupInfo;
  @NotEmpty private String workDir;
  private Set<String> publishStepConnectorIdentifier;

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
