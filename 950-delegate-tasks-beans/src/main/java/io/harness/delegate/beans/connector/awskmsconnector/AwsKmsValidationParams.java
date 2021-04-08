package io.harness.delegate.beans.connector.awskmsconnector;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import lombok.Builder;
import lombok.Value;

import java.util.*;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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

  @Override//TODO: Shashank: Review this logic
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
    if (awsKmsConnectorDTO.getCredential().getCredentialType().equals(AwsKmsCredentialType.ASSUME_IAM_ROLE))
        delegateSelectors = ((AwsKmsCredentialSpecAssumeIAMDTO) config).getDelegateSelectors();
    else if (awsKmsConnectorDTO.getCredential().getCredentialType().equals(AwsKmsCredentialType.ASSUME_STS_ROLE))
        delegateSelectors = ((AwsKmsCredentialSpecAssumeSTSDTO) config).getDelegateSelectors();
    return delegateSelectors;
  }
}
