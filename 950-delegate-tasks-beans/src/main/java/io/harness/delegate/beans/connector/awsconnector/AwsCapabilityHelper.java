package io.harness.delegate.beans.connector.awsconnector;

import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, AwsConnectorDTO awsConnectorDTO) {
    AwsCredentialDTO credential = awsConnectorDTO.getCredential();
    if (credential.getAwsCredentialType() == INHERIT_FROM_DELEGATE) {
      AwsInheritFromDelegateSpecDTO config = (AwsInheritFromDelegateSpecDTO) credential.getConfig();
      return Collections.singletonList(SelectorCapability.builder().selectors(config.getDelegateSelectors()).build());
    } else if (credential.getAwsCredentialType() == MANUAL_CREDENTIALS) {
      final String AWS_URL = "https://aws.amazon.com/";
      return Arrays.asList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));
    } else {
      throw new UnknownEnumTypeException("AWS Credential Type", String.valueOf(credential.getAwsCredentialType()));
    }
  }
}
