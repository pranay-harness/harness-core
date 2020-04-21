package io.harness.beans.environment;

import io.harness.beans.environment.pod.CIPodSetupInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Data
@Value
@Builder
public class CIK8BuildJobEnvInfo implements CIBuildJobEnvInfo {
  private CIPodsSetupInfo ciPodsSetupInfo;

  @Override
  public Type getType() {
    return Type.K8;
  }

  @Builder
  public static final class CIPodsSetupInfo {
    private List<CIPodSetupInfo> pods = new ArrayList<>();
  }
}
