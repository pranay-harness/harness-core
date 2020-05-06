package io.harness.beans.environment.pod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.environment.pod.container.CIContainerDefinitionInfo;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Data
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIPodSetupInfo {
  private PodSetupParams podSetupParams;

  @Builder
  public static final class PodSetupParams {
    private List<CIContainerDefinitionInfo> containerInfos;
  }
}
