package io.harness.delegate.beans.connector.awscodecommitconnector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthentication;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnector;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsCodeCommitCapabilityHelper extends ConnectorCapabilityBaseHelper {
  private static final String AWS_URL = "https://aws.amazon.com/";

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    AwsCodeCommitConnector awsCodeCommitConnector = (AwsCodeCommitConnector) connectorConfigDTO;
    AwsCodeCommitAuthentication authentication = awsCodeCommitConnector.getAuthentication();
    if (authentication.getAuthType() == AwsCodeCommitAuthType.HTTPS) {
      capabilityList.add(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));
    }
    populateDelegateSelectorCapability(capabilityList, awsCodeCommitConnector.getDelegateSelectors());
    return capabilityList;
  }
}
