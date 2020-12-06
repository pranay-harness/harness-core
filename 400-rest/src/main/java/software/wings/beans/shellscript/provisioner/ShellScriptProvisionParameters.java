package software.wings.beans.shellscript.provisioner;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShellScriptProvisionParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  @Expression(ALLOW_SECRETS) private String scriptBody;
  private long timeoutInMillis;
  private Map<String, String> textVariables;
  private Map<String, EncryptedDataDetail> encryptedVariables;
  private String entityId;
  private String workflowExecutionId;

  private String accountId;
  private String appId;
  private String activityId;
  private String commandUnit;

  /*
  Name of the variable which contains the file path
   */
  private String outputPathKey;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (isNotEmpty(encryptedVariables)) {
      for (EncryptedDataDetail encryptedDataDetail : encryptedVariables.values()) {
        executionCapabilities.addAll(CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            Arrays.asList(encryptedDataDetail), maskingEvaluator));
      }
    }

    return executionCapabilities;
  }
}
