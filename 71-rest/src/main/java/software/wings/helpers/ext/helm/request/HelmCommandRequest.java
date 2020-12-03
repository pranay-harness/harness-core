package software.wings.helpers.ext.helm.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.k8s.model.HelmVersion.V2;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmCommandFlag;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;
import software.wings.delegatetasks.validation.capabilities.HelmCommandCapability;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@AllArgsConstructor
public class HelmCommandRequest implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
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
  @Expression(ALLOW_SECRETS) private String commandFlags;
  @Expression(ALLOW_SECRETS) private HelmCommandFlag helmCommandFlag;
  private K8sDelegateManifestConfig repoConfig;
  @Builder.Default private HelmVersion helmVersion = V2;
  private String ocPath;
  private String workingDir;
  @Expression(ALLOW_SECRETS) private List<String> variableOverridesYamlFiles;
  private GitFileConfig gitFileConfig;
  private boolean k8SteadyStateCheckEnabled;
  private boolean deprecateFabric8Enabled;

  public HelmCommandRequest(HelmCommandType helmCommandType) {
    this.helmCommandType = helmCommandType;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(HelmCommandCapability.builder().commandRequest(this).build());
    if (gitConfig != null) {
      executionCapabilities.add(GitConnectionCapability.builder()
                                    .gitConfig(gitConfig)
                                    .settingAttribute(gitConfig.getSshSettingAttribute())
                                    .encryptedDataDetails(getEncryptedDataDetails())
                                    .build());
    }
    if (containerServiceParams != null) {
      executionCapabilities.addAll(containerServiceParams.fetchRequiredExecutionCapabilities());
    }
    return executionCapabilities;
  }

  public enum HelmCommandType { INSTALL, ROLLBACK, LIST_RELEASE, RELEASE_HISTORY }
}
