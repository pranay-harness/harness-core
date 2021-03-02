package io.harness.delegate.task.terraform;

import static io.harness.delegate.task.terraform.TerraformTaskType.APPLY;
import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TerraformApplyRequest implements TerraformRequest {
  String tfConfig;
  @Expression(DISALLOW_SECRETS) List<String> varFileContents;
  @Expression(DISALLOW_SECRETS) Map<String, String> variables;
  Map<String, EncryptedDataDetail> encryptedVariables;
  @Expression(DISALLOW_SECRETS) Map<String, String> backendConfigs;
  Map<String, EncryptedDataDetail> encryptedBackendConfigs;
  @Expression(DISALLOW_SECRETS) Map<String, String> environmentVariables;
  Map<String, EncryptedDataDetail> encryptedEnvironmentVariables;
  int timeoutIntervalMinutes;
  @Expression(DISALLOW_SECRETS) String workspace;
  @Expression(DISALLOW_SECRETS) List<String> targets;

  @Override
  public TerraformTaskType getTaskType() {
    return APPLY;
  }
}
