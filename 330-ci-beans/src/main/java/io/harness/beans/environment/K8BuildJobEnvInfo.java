package io.harness.beans.environment;

import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.delegate.beans.ci.pod.EnvVariableEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

/**
 * Stores K8 specific data to setup Pod for running CI job
 */

@Data
@Value
@Builder
@TypeAlias("k8BuildJobEnvInfo")
public class K8BuildJobEnvInfo implements BuildJobEnvInfo {
  @NotEmpty private PodsSetupInfo podsSetupInfo;
  @NotEmpty private String workDir;
  private Map<String, ConnectorConversionInfo> stepConnectorRefs;

  @Override
  public Type getType() {
    return Type.K8;
  }

  @Data
  @Builder
  public static final class PodsSetupInfo {
    private List<PodSetupInfo> podSetupInfoList = new ArrayList<>();
  }

  @Data
  @Builder
  public static final class ConnectorConversionInfo {
    private String connectorRef;
    @Singular("envToSecretEntry") private Map<EnvVariableEnum, String> envToSecretsMap;
  }
}
