package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class AwsKmsValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  private static final String TASK_SELECTORS = "Task Selectors";
  AwsKmsConnectorDTO awsKmsConnectorDTO;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.AWS_KMS;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>(
        Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(
            awsKmsConnectorDTO.getRegion(), maskingEvaluator)));

    Set<String> delegateSelectors = getDelegateSelectors();

    if (isNotEmpty(delegateSelectors)) {
      executionCapabilities.add(
          SelectorCapability.builder().selectors(delegateSelectors).selectorOrigin(TASK_SELECTORS).build());
    }

    return executionCapabilities;
  }

  private Set<String> getDelegateSelectors() {
    Set<String> delegateSelectors = new HashSet<>();
    AwsKmsCredentialSpecDTO config = awsKmsConnectorDTO.getCredential().getConfig();
    AwsKmsCredentialType credentialType = awsKmsConnectorDTO.getCredential().getCredentialType();
    if (AwsKmsCredentialType.ASSUME_IAM_ROLE.equals(credentialType)) {
      delegateSelectors = ((AwsKmsCredentialSpecAssumeIAMDTO) config).getDelegateSelectors();
    } else if (AwsKmsCredentialType.ASSUME_STS_ROLE.equals(credentialType)) {
      delegateSelectors = ((AwsKmsCredentialSpecAssumeSTSDTO) config).getDelegateSelectors();
    }
    return delegateSelectors;
  }
}
