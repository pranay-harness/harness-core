package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.SecretManagerCapabilities.CREATE_PARAMETERIZED_SECRET;
import static io.harness.security.encryption.SecretManagerType.CUSTOM;

import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerValidationUtils.buildShellScriptParameters;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerCapabilities;
import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;

import software.wings.annotation.EncryptableSetting;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;

import com.github.reinert.jjschema.Attributes;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"customSecretsManagerShellScript", "remoteHostConnector"})
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "CustomSecretsManagerConfigKeys")
public class CustomSecretsManagerConfig extends SecretManagerConfig {
  @NonNull @NotEmpty @Attributes(title = "Name") private String name;
  @NonNull @NotEmpty @Attributes(title = "Template Shell Script") private String templateId;
  @NonNull @Attributes(title = "Delegate Selectors") private List<String> delegateSelectors;
  @NonNull @Attributes(title = "Test Parameters") private Set<EncryptedDataParams> testVariables;
  @Attributes(title = "Execute on Delegate") private boolean executeOnDelegate;
  @Attributes(title = "Templatize Connector") private boolean isConnectorTemplatized;
  @Attributes(title = "Target Host") private String host;
  @Attributes(title = "Command Path") private String commandPath;
  @Attributes(title = "Connection Attributes Reference Id") private String connectorId;
  private CustomSecretsManagerShellScript customSecretsManagerShellScript;
  private EncryptableSetting remoteHostConnector;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (executeOnDelegate) {
      if (customSecretsManagerShellScript.getScriptType() == POWERSHELL) {
        return Collections.singletonList(ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
            "DELEGATE_POWERSHELL", Arrays.asList("/bin/sh", "-c", "pwsh -Version")));
      }
      return new ArrayList<>();
    }

    return Collections.singletonList(
        ShellConnectionCapability.builder().shellScriptParameters(buildShellScriptParameters(this)).build());
  }

  @Override
  public void maskSecrets() {
    // Nothing to mask
  }

  @Override
  public String getEncryptionServiceUrl() {
    return null;
  }

  @Override
  public String getValidationCriteria() {
    if (executeOnDelegate) {
      return "localhost";
    } else {
      return host;
    }
  }

  @Override
  public SecretManagerType getType() {
    return CUSTOM;
  }

  @Override
  public EncryptionType getEncryptionType() {
    return EncryptionType.CUSTOM;
  }

  @Override
  public List<SecretManagerCapabilities> getSecretManagerCapabilities() {
    return Lists.newArrayList(CREATE_PARAMETERIZED_SECRET);
  }

  @Override
  public SecretManagerConfigDTO toDTO(boolean maskSecrets) {
    throw new UnsupportedOperationException();
  }
}
