package software.wings.helpers.ext.helm.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.GitConfig;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@AllArgsConstructor
public class HelmCommandRequest implements ExecutionCapabilityDemander {
  @NotEmpty private HelmCommandType helmCommandType;
  private String accountId;
  private String appId;
  private String kubeConfigLocation;
  private String commandName;
  private String activityId;
  private ContainerServiceParams containerServiceParams;
  private String releaseName;
  private HelmChartSpecification chartSpecification;
  private String repoName;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  @JsonIgnore private transient LogCallback executionLogCallback;
  private String commandFlags;
  private K8sDelegateManifestConfig repoConfig;

  public HelmCommandRequest(HelmCommandType helmCommandType) {
    this.helmCommandType = helmCommandType;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.addAll(gitConfig.fetchRequiredExecutionCapabilities());
    executionCapabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities());
    return executionCapabilities;
  }

  public enum HelmCommandType { INSTALL, ROLLBACK, LIST_RELEASE, RELEASE_HISTORY }
}
