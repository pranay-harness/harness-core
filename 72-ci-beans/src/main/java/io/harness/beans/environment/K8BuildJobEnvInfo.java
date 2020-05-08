package io.harness.beans.environment;

import io.harness.beans.environment.pod.PodSetupInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Data
@Value
@Builder
public class K8BuildJobEnvInfo implements BuildJobEnvInfo {
  private PodsSetupInfo podsSetupInfo;

  @Override
  public Type getType() {
    return Type.K8;
  }

  @Builder
  public static final class PodsSetupInfo {
    private List<PodSetupInfo> pods = new ArrayList<>();
  }
}
